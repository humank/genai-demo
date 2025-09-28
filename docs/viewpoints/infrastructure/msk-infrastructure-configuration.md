# Infrastructure Viewpoint - MSK Infrastructure Configuration

**Document Version**: 2.0  
**Last Updated**: September 28, 2025 11:39 PM (Taipei Time)  
**Author**: Infrastructure Team + DevOps Team  
**Status**: Active

## 📋 Overview

This document provides detailed description of MSK (Amazon Managed Streaming for Apache Kafka) infrastructure configuration, including CDK implementation details, cluster topology, network security configuration, IAM roles, and auto-scaling configuration.

## 🏗️ CDK Infrastructure Implementation

### MSK Stack Core Configuration

#### Main MSK Stack Implementation
```typescript
// infrastructure/src/stacks/msk-stack.ts
export class MSKStack extends cdk.Stack {
  public readonly mskCluster: msk.CfnCluster;
  public readonly mskConfiguration: msk.CfnConfiguration;
  
  constructor(scope: Construct, id: string, props: MSKStackProps) {
    super(scope, id, props);
    
    // Create MSK configuration
    this.mskConfiguration = this.createMSKConfiguration();
    
    // Create MSK cluster
    this.mskCluster = this.createMSKCluster(props.vpc, props.subnets);
    
    // Setup monitoring and logging
    this.setupMonitoringAndLogging();
    
    // Setup security configuration
    this.setupSecurityConfiguration();
  }
}
```

#### MSK Cluster Configuration Details
```typescript
private createMSKCluster(vpc: ec2.IVpc, subnets: ec2.ISubnet[]): msk.CfnCluster {
  return new msk.CfnCluster(this, 'MSKCluster', {
    clusterName: 'genai-demo-msk-cluster',
    kafkaVersion: '2.8.1',
    numberOfBrokerNodes: 3,
    
    brokerNodeGroupInfo: {
      instanceType: 'm5.large',
      clientSubnets: subnets.map(subnet => subnet.subnetId),
      securityGroups: [this.mskSecurityGroup.securityGroupId],
      storageInfo: {
        ebsStorageInfo: {
          volumeSize: 100,
          provisionedThroughput: {
            enabled: true,
            volumeThroughput: 250
          }
        }
      }
    },
    
    configurationInfo: {
      arn: this.mskConfiguration.attrArn,
      revision: 1
    },
    
    encryptionInfo: {
      encryptionAtRest: {
        dataVolumeKmsKeyId: this.kmsKey.keyId
      },
      encryptionInTransit: {
        clientBroker: 'TLS',
        inCluster: true
      }
    }
  });
}
```

### MSK Configuration Parameters

#### Kafka Server Configuration
```typescript
private createMSKConfiguration(): msk.CfnConfiguration {
  const kafkaConfig = [
    'auto.create.topics.enable=false',
    'default.replication.factor=3',
    'min.insync.replicas=2',
    'num.partitions=12',
    'log.retention.hours=168',
    'log.segment.bytes=1073741824',
    'log.retention.check.interval.ms=300000',
    'compression.type=gzip',
    'message.max.bytes=1000000',
    'replica.lag.time.max.ms=30000',
    'num.network.threads=8',
    'num.io.threads=16',
    'socket.send.buffer.bytes=102400',
    'socket.receive.buffer.bytes=102400',
    'socket.request.max.bytes=104857600',
    'num.replica.fetchers=4',
    'replica.fetch.max.bytes=1048576',
    'group.initial.rebalance.delay.ms=3000',
    'offsets.topic.replication.factor=3',
    'transaction.state.log.replication.factor=3',
    'transaction.state.log.min.isr=2'
  ].join('\n');

  return new msk.CfnConfiguration(this, 'MSKConfiguration', {
    name: 'genai-demo-msk-config',
    description: 'MSK configuration for GenAI Demo application',
    kafkaVersionsList: ['2.8.1'],
    serverProperties: kafkaConfig
  });
}
```

#### JVM and Performance Tuning
```typescript
private getJVMConfiguration(): string {
  return [
    '# JVM Heap Settings',
    '-Xmx6g',
    '-Xms6g',
    '# GC Settings',
    '-XX:+UseG1GC',
    '-XX:MaxGCPauseMillis=20',
    '-XX:InitiatingHeapOccupancyPercent=35',
    '-XX:+ExplicitGCInvokesConcurrent',
    '# Performance Settings',
    '-server',
    '-Djava.awt.headless=true',
    '-Dcom.sun.management.jmxremote=true',
    '-Dcom.sun.management.jmxremote.authenticate=false',
    '-Dcom.sun.management.jmxremote.ssl=false'
  ].join('\n');
}
```

## 🌐 Network Security Configuration

### VPC and Subnet Design

#### Network Topology
```
┌─────────────────────────────────────────────────────────────┐
│                    VPC (10.0.0.0/16)                       │
├─────────────────────────────────────────────────────────────┤
│  AZ-1a              │  AZ-1b              │  AZ-1c          │
│  ┌─────────────────┐ │ ┌─────────────────┐ │ ┌─────────────┐ │
│  │Private Subnet   │ │ │Private Subnet   │ │ │Private Subnet│ │
│  │10.0.1.0/24      │ │ │10.0.2.0/24      │ │ │10.0.3.0/24   │ │
│  │MSK Broker-1     │ │ │MSK Broker-2     │ │ │MSK Broker-3  │ │
│  └─────────────────┘ │ └─────────────────┘ │ └─────────────┘ │
│  ┌─────────────────┐ │ ┌─────────────────┐ │ ┌─────────────┐ │
│  │Private Subnet   │ │ │Private Subnet   │ │ │Private Subnet│ │
│  │10.0.11.0/24     │ │ │10.0.12.0/24     │ │ │10.0.13.0/24  │ │
│  │EKS Nodes        │ │ │EKS Nodes        │ │ │EKS Nodes     │ │
│  └─────────────────┘ │ └─────────────────┘ │ └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### VPC Configuration Implementation
```typescript
export class NetworkStack extends cdk.Stack {
  public readonly vpc: ec2.Vpc;
  public readonly mskSubnets: ec2.ISubnet[];
  public readonly eksSubnets: ec2.ISubnet[];

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create VPC
    this.vpc = new ec2.Vpc(this, 'GenAIDemoVPC', {
      cidr: '10.0.0.0/16',
      maxAzs: 3,
      enableDnsHostnames: true,
      enableDnsSupport: true,
      
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: 'MSK-Private',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
        {
          cidrMask: 24,
          name: 'EKS-Private', 
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
        {
          cidrMask: 28,
          name: 'Public',
          subnetType: ec2.SubnetType.PUBLIC,
        }
      ],
      
      natGateways: 3, // One NAT Gateway per AZ
    });

    // Get MSK dedicated subnets
    this.mskSubnets = this.vpc.selectSubnets({
      subnetGroupName: 'MSK-Private'
    }).subnets;

    // Get EKS dedicated subnets  
    this.eksSubnets = this.vpc.selectSubnets({
      subnetGroupName: 'EKS-Private'
    }).subnets;
  }
}
```

### Security Group Configuration

#### MSK Security Group
```typescript
private createMSKSecurityGroup(vpc: ec2.IVpc): ec2.SecurityGroup {
  const mskSecurityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
    vpc: vpc,
    description: 'Security group for MSK cluster',
    allowAllOutbound: false
  });

  // Kafka client connections (9092, 9094, 9098)
  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(9092),
    'Kafka plaintext client connections'
  );

  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(9094),
    'Kafka TLS client connections'
  );

  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(9098),
    'Kafka SASL/SCRAM connections'
  );

  // Zookeeper connections (2181, 2182)
  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(2181),
    'Zookeeper plaintext connections'
  );

  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(2182),
    'Zookeeper TLS connections'
  );

  // JMX monitoring (11001, 11002)
  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(11001),
    'JMX monitoring'
  );

  // Allow inter-broker communication
  mskSecurityGroup.addIngressRule(
    mskSecurityGroup,
    ec2.Port.allTraffic(),
    'Inter-broker communication'
  );

  // Egress rules
  mskSecurityGroup.addEgressRule(
    ec2.Peer.anyIpv4(),
    ec2.Port.tcp(443),
    'HTTPS outbound for AWS services'
  );

  mskSecurityGroup.addEgressRule(
    ec2.Peer.anyIpv4(),
    ec2.Port.tcp(53),
    'DNS resolution'
  );

  return mskSecurityGroup;
}
```

#### EKS to MSK Connection Security Group
```typescript
private createEKSToMSKSecurityGroup(vpc: ec2.IVpc, mskSecurityGroup: ec2.SecurityGroup): ec2.SecurityGroup {
  const eksToMskSG = new ec2.SecurityGroup(this, 'EKSToMSKSecurityGroup', {
    vpc: vpc,
    description: 'Security group for EKS to MSK connections',
    allowAllOutbound: true
  });

  // Allow EKS nodes to connect to MSK
  mskSecurityGroup.addIngressRule(
    eksToMskSG,
    ec2.Port.tcp(9092),
    'EKS to MSK plaintext'
  );

  mskSecurityGroup.addIngressRule(
    eksToMskSG,
    ec2.Port.tcp(9094),
    'EKS to MSK TLS'
  );

  mskSecurityGroup.addIngressRule(
    eksToMskSG,
    ec2.Port.tcp(9098),
    'EKS to MSK SASL/SCRAM'
  );

  return eksToMskSG;
}
```

## 🔐 IAM Roles and Permissions Configuration

### MSK Service Roles

#### MSK Cluster Service Role
```typescript
private createMSKServiceRole(): iam.Role {
  const mskServiceRole = new iam.Role(this, 'MSKServiceRole', {
    assumedBy: new iam.ServicePrincipal('kafka.amazonaws.com'),
    description: 'Service role for MSK cluster operations',
    managedPolicies: [
      iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/MSKServiceRolePolicy')
    ],
    inlinePolicies: {
      MSKClusterPolicy: new iam.PolicyDocument({
        statements: [
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'ec2:CreateNetworkInterface',
              'ec2:DescribeNetworkInterfaces',
              'ec2:CreateNetworkInterfacePermission',
              'ec2:AttachNetworkInterface',
              'ec2:DetachNetworkInterface',
              'ec2:DeleteNetworkInterface'
            ],
            resources: ['*']
          }),
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'kms:Decrypt',
              'kms:GenerateDataKey',
              'kms:CreateGrant',
              'kms:DescribeKey'
            ],
            resources: [this.kmsKey.keyArn]
          })
        ]
      })
    }
  });

  return mskServiceRole;
}
```

#### Application MSK Access Role (IRSA)
```typescript
private createApplicationMSKRole(eksCluster: eks.Cluster): iam.Role {
  const appMSKRole = new iam.Role(this, 'ApplicationMSKRole', {
    assumedBy: new iam.WebIdentityPrincipal(
      eksCluster.openIdConnectProvider.openIdConnectProviderArn,
      {
        'StringEquals': {
          [`${eksCluster.clusterOpenIdConnectIssuer}:sub`]: 'system:serviceaccount:default:msk-service-account',
          [`${eksCluster.clusterOpenIdConnectIssuer}:aud`]: 'sts.amazonaws.com'
        }
      }
    ),
    description: 'IAM role for application MSK access via IRSA',
    inlinePolicies: {
      MSKClientPolicy: new iam.PolicyDocument({
        statements: [
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'kafka-cluster:Connect',
              'kafka-cluster:AlterCluster',
              'kafka-cluster:DescribeCluster'
            ],
            resources: [this.mskCluster.attrArn]
          }),
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'kafka-cluster:*Topic*',
              'kafka-cluster:WriteData',
              'kafka-cluster:ReadData'
            ],
            resources: [`${this.mskCluster.attrArn}/topic/*`]
          }),
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'kafka-cluster:AlterGroup',
              'kafka-cluster:DescribeGroup'
            ],
            resources: [`${this.mskCluster.attrArn}/group/*`]
          })
        ]
      })
    }
  });

  return appMSKRole;
}
```

### Monitoring and Logging IAM Permissions

#### CloudWatch and X-Ray Permissions
```typescript
private createMonitoringRole(): iam.Role {
  return new iam.Role(this, 'MSKMonitoringRole', {
    assumedBy: new iam.ServicePrincipal('kafka.amazonaws.com'),
    description: 'Role for MSK monitoring and logging',
    inlinePolicies: {
      MonitoringPolicy: new iam.PolicyDocument({
        statements: [
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'logs:CreateLogGroup',
              'logs:CreateLogStream',
              'logs:PutLogEvents',
              'logs:DescribeLogGroups',
              'logs:DescribeLogStreams'
            ],
            resources: [
              `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/msk/*`
            ]
          }),
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'cloudwatch:PutMetricData',
              'cloudwatch:GetMetricStatistics',
              'cloudwatch:ListMetrics'
            ],
            resources: ['*']
          }),
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
              'xray:PutTraceSegments',
              'xray:PutTelemetryRecords',
              'xray:GetSamplingRules',
              'xray:GetSamplingTargets'
            ],
            resources: ['*']
          })
        ]
      })
    }
  });
}
```

## 🔧 Auto-Scaling Configuration

### CloudWatch Metrics and Alarms

#### Auto-Scaling Triggers
```typescript
private setupAutoScaling(): void {
  // CPU usage alarm
  const cpuAlarm = new cloudwatch.Alarm(this, 'MSKCPUAlarm', {
    alarmName: 'MSK-CPU-High',
    alarmDescription: 'MSK broker CPU usage is high',
    metric: new cloudwatch.Metric({
      namespace: 'AWS/Kafka',
      metricName: 'CpuUser',
      dimensionsMap: {
        'Cluster Name': this.mskCluster.clusterName
      },
      statistic: 'Average'
    }),
    threshold: 70,
    evaluationPeriods: 2,
    comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD
  });

  // Memory usage alarm
  const memoryAlarm = new cloudwatch.Alarm(this, 'MSKMemoryAlarm', {
    alarmName: 'MSK-Memory-High',
    alarmDescription: 'MSK broker memory usage is high',
    metric: new cloudwatch.Metric({
      namespace: 'AWS/Kafka',
      metricName: 'MemoryUsed',
      dimensionsMap: {
        'Cluster Name': this.mskCluster.clusterName
      },
      statistic: 'Average'
    }),
    threshold: 80,
    evaluationPeriods: 2,
    comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD
  });

  // Disk usage alarm
  const diskAlarm = new cloudwatch.Alarm(this, 'MSKDiskAlarm', {
    alarmName: 'MSK-Disk-High',
    alarmDescription: 'MSK broker disk usage is high',
    metric: new cloudwatch.Metric({
      namespace: 'AWS/Kafka',
      metricName: 'KafkaDataLogsDiskUsed',
      dimensionsMap: {
        'Cluster Name': this.mskCluster.clusterName
      },
      statistic: 'Maximum'
    }),
    threshold: 80,
    evaluationPeriods: 1,
    comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD
  });
}
```

#### Auto-Scaling Lambda Function
```typescript
private createAutoScalingFunction(): lambda.Function {
  return new lambda.Function(this, 'MSKAutoScalingFunction', {
    functionName: 'msk-auto-scaling',
    runtime: lambda.Runtime.PYTHON_3_11,
    handler: 'index.lambda_handler',
    timeout: cdk.Duration.minutes(5),
    memorySize: 256,
    description: 'Auto-scaling function for MSK cluster',
    code: lambda.Code.fromInline(`
import json
import boto3
from datetime import datetime

kafka_client = boto3.client('kafka')
cloudwatch = boto3.client('cloudwatch')

def lambda_handler(event, context):
    """
    MSK Auto-scaling handler
    """
    try:
        # Parse CloudWatch alarm
        alarm_data = json.loads(event['Records'][0]['Sns']['Message'])
        alarm_name = alarm_data['AlarmName']
        new_state = alarm_data['NewStateValue']
        
        if new_state == 'ALARM':
            if 'CPU' in alarm_name or 'Memory' in alarm_name:
                scale_compute_resources(alarm_data)
            elif 'Disk' in alarm_name:
                scale_storage_resources(alarm_data)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Auto-scaling completed',
                'alarm': alarm_name,
                'action': 'scaled' if new_state == 'ALARM' else 'no_action'
            })
        }
        
    except Exception as e:
        print(f"Error in auto-scaling: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def scale_compute_resources(alarm_data):
    """Scale compute resources"""
    cluster_arn = get_cluster_arn_from_alarm(alarm_data)
    
    # Get current cluster configuration
    response = kafka_client.describe_cluster(ClusterArn=cluster_arn)
    current_instance_type = response['ClusterInfo']['BrokerNodeGroupInfo']['InstanceType']
    
    # Upgrade instance type
    upgrade_map = {
        'm5.large': 'm5.xlarge',
        'm5.xlarge': 'm5.2xlarge',
        'm5.2xlarge': 'm5.4xlarge'
    }
    
    if current_instance_type in upgrade_map:
        new_instance_type = upgrade_map[current_instance_type]
        
        kafka_client.update_broker_type(
            ClusterArn=cluster_arn,
            CurrentVersion=response['ClusterInfo']['CurrentVersion'],
            TargetInstanceType=new_instance_type
        )
        
        print(f"Upgraded instance type from {current_instance_type} to {new_instance_type}")

def scale_storage_resources(alarm_data):
    """Scale storage resources"""
    cluster_arn = get_cluster_arn_from_alarm(alarm_data)
    
    # Get current storage configuration
    response = kafka_client.describe_cluster(ClusterArn=cluster_arn)
    current_volume_size = response['ClusterInfo']['BrokerNodeGroupInfo']['StorageInfo']['EBSStorageInfo']['VolumeSize']
    
    # Increase storage by 50%
    new_volume_size = int(current_volume_size * 1.5)
    
    kafka_client.update_broker_storage(
        ClusterArn=cluster_arn,
        CurrentVersion=response['ClusterInfo']['CurrentVersion'],
        TargetBrokerEBSVolumeInfo=[{
            'KafkaBrokerNodeId': str(i),
            'VolumeSizeGB': new_volume_size
        } for i in range(response['ClusterInfo']['NumberOfBrokerNodes'])]
    )
    
    print(f"Scaled storage from {current_volume_size}GB to {new_volume_size}GB")

def get_cluster_arn_from_alarm(alarm_data):
    """Extract cluster ARN from alarm data"""
    # Extract cluster name from alarm dimensions
    dimensions = alarm_data.get('Trigger', {}).get('Dimensions', [])
    cluster_name = None
    
    for dim in dimensions:
        if dim['name'] == 'Cluster Name':
            cluster_name = dim['value']
            break
    
    if not cluster_name:
        raise ValueError("Could not find cluster name in alarm data")
    
    # Build cluster ARN
    return f"arn:aws:kafka:{boto3.Session().region_name}:{boto3.client('sts').get_caller_identity()['Account']}:cluster/{cluster_name}"
`),
    environment: {
      CLUSTER_ARN: this.mskCluster.attrArn
    }
  });
}
```

## 📊 Monitoring and Logging Configuration

### CloudWatch Log Groups
```typescript
private setupLogging(): void {
  // MSK Broker logs
  const brokerLogGroup = new logs.LogGroup(this, 'MSKBrokerLogGroup', {
    logGroupName: '/aws/msk/broker-logs',
    retention: logs.RetentionDays.ONE_WEEK,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });

  // MSK Controller logs
  const controllerLogGroup = new logs.LogGroup(this, 'MSKControllerLogGroup', {
    logGroupName: '/aws/msk/controller-logs', 
    retention: logs.RetentionDays.ONE_WEEK,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });

  // Application logs
  const appLogGroup = new logs.LogGroup(this, 'MSKAppLogGroup', {
    logGroupName: '/aws/msk/application-logs',
    retention: logs.RetentionDays.TWO_WEEKS,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });
}
```

### Performance Metrics Dashboard
```typescript
private createPerformanceDashboard(): cloudwatch.Dashboard {
  return new cloudwatch.Dashboard(this, 'MSKPerformanceDashboard', {
    dashboardName: 'GenAI-Demo-MSK-Performance',
    widgets: [
      [
        new cloudwatch.GraphWidget({
          title: 'MSK Cluster CPU Usage',
          left: [
            new cloudwatch.Metric({
              namespace: 'AWS/Kafka',
              metricName: 'CpuUser',
              dimensionsMap: {
                'Cluster Name': this.mskCluster.clusterName
              }
            })
          ],
          width: 12,
          height: 6
        })
      ],
      [
        new cloudwatch.GraphWidget({
          title: 'MSK Cluster Memory Usage',
          left: [
            new cloudwatch.Metric({
              namespace: 'AWS/Kafka',
              metricName: 'MemoryUsed',
              dimensionsMap: {
                'Cluster Name': this.mskCluster.clusterName
              }
            })
          ],
          width: 12,
          height: 6
        })
      ],
      [
        new cloudwatch.GraphWidget({
          title: 'Kafka Message Throughput',
          left: [
            new cloudwatch.Metric({
              namespace: 'AWS/Kafka',
              metricName: 'MessagesInPerSec',
              dimensionsMap: {
                'Cluster Name': this.mskCluster.clusterName
              }
            }),
            new cloudwatch.Metric({
              namespace: 'AWS/Kafka',
              metricName: 'BytesInPerSec',
              dimensionsMap: {
                'Cluster Name': this.mskCluster.clusterName
              }
            })
          ],
          width: 12,
          height: 6
        })
      ]
    ]
  });
}
```

---

**Document Status**: ✅ Complete  
**Next Step**: Review [Event Configuration](./event-configuration.md) for Kafka event processing setup  
**Related Documents**: 
- [Infrastructure Viewpoint - Event Configuration](./event-configuration.md)
- [Infrastructure Viewpoint - AWS Resource Architecture](./aws-resource-architecture.md)
- [Architecture Viewpoint - Event-Driven Design](../architecture/event-driven-design.md)