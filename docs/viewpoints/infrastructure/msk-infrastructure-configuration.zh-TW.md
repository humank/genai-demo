# MSK Infrastructure Configuration

**文檔版本**: 2.0  
**最後更新**: 2025年9月24日 下午10:25 (台北時間)  
**負責團隊**: 基礎設施團隊 + DevOps 團隊

## 📋 概述

本文檔詳細描述了 MSK (Amazon Managed Streaming for Apache Kafka) 基礎設施配置，包括 CDK 實作細節、集群拓撲、網路安全配置、IAM 角色和自動擴展配置。

## 🏗️ CDK 基礎設施實作

### MSK Stack 核心配置

#### 主要 MSK Stack 實作
```typescript
// infrastructure/src/stacks/msk-stack.ts
export class MSKStack extends cdk.Stack {
  public readonly mskCluster: msk.CfnCluster;
  public readonly mskConfiguration: msk.CfnConfiguration;
  
  constructor(scope: Construct, id: string, props: MSKStackProps) {
    super(scope, id, props);
    
    // 創建 MSK 配置
    this.mskConfiguration = this.createMSKConfiguration();
    
    // 創建 MSK 集群
    this.mskCluster = this.createMSKCluster(props.vpc, props.subnets);
    
    // 配置監控和日誌
    this.setupMonitoringAndLogging();
    
    // 配置安全設定
    this.setupSecurityConfiguration();
  }
}
```

#### MSK 集群配置詳細
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
```### MSK 配置
參數

#### Kafka 伺服器配置
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

#### JVM 和效能調優
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

## 🌐 網路安全配置

### VPC 和子網路設計

#### 網路拓撲
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

#### VPC 配置實作
```typescript
export class NetworkStack extends cdk.Stack {
  public readonly vpc: ec2.Vpc;
  public readonly mskSubnets: ec2.ISubnet[];
  public readonly eksSubnets: ec2.ISubnet[];

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // 創建 VPC
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
      
      natGateways: 3, // 每個 AZ 一個 NAT Gateway
    });

    // 取得 MSK 專用子網路
    this.mskSubnets = this.vpc.selectSubnets({
      subnetGroupName: 'MSK-Private'
    }).subnets;

    // 取得 EKS 專用子網路  
    this.eksSubnets = this.vpc.selectSubnets({
      subnetGroupName: 'EKS-Private'
    }).subnets;
  }
}
```#
## 安全群組配置

#### MSK 安全群組
```typescript
private createMSKSecurityGroup(vpc: ec2.IVpc): ec2.SecurityGroup {
  const mskSecurityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
    vpc: vpc,
    description: 'Security group for MSK cluster',
    allowAllOutbound: false
  });

  // Kafka 客戶端連接 (9092, 9094, 9098)
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

  // Zookeeper 連接 (2181, 2182)
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

  // JMX 監控 (11001, 11002)
  mskSecurityGroup.addIngressRule(
    ec2.Peer.ipv4(vpc.vpcCidrBlock),
    ec2.Port.tcp(11001),
    'JMX monitoring'
  );

  // 允許內部 Broker 間通信
  mskSecurityGroup.addIngressRule(
    mskSecurityGroup,
    ec2.Port.allTraffic(),
    'Inter-broker communication'
  );

  // 出站規則
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

#### EKS 到 MSK 連接安全群組
```typescript
private createEKSToMSKSecurityGroup(vpc: ec2.IVpc, mskSecurityGroup: ec2.SecurityGroup): ec2.SecurityGroup {
  const eksToMskSG = new ec2.SecurityGroup(this, 'EKSToMSKSecurityGroup', {
    vpc: vpc,
    description: 'Security group for EKS to MSK connections',
    allowAllOutbound: true
  });

  // 允許 EKS 節點連接到 MSK
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

## 🔐 IAM 角色和權限配置

### MSK 服務角色

#### MSK 集群服務角色
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

#### 應用程式 MSK 存取角色 (IRSA)
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
```##
# 監控和日誌 IAM 權限

#### CloudWatch 和 X-Ray 權限
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

## 🔧 自動擴展配置

### CloudWatch 指標和警報

#### 自動擴展觸發器
```typescript
private setupAutoScaling(): void {
  // CPU 使用率警報
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

  // 記憶體使用率警報
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

  // 磁碟使用率警報
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

#### 自動擴展 Lambda 函數
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
        # 解析 CloudWatch 警報
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
    """擴展計算資源"""
    cluster_arn = get_cluster_arn_from_alarm(alarm_data)
    
    # 獲取當前集群配置
    response = kafka_client.describe_cluster(ClusterArn=cluster_arn)
    current_instance_type = response['ClusterInfo']['BrokerNodeGroupInfo']['InstanceType']
    
    # 升級實例類型
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
    """擴展儲存資源"""
    cluster_arn = get_cluster_arn_from_alarm(alarm_data)
    
    # 獲取當前儲存配置
    response = kafka_client.describe_cluster(ClusterArn=cluster_arn)
    current_volume_size = response['ClusterInfo']['BrokerNodeGroupInfo']['StorageInfo']['EBSStorageInfo']['VolumeSize']
    
    # 增加 50% 儲存空間
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
    """從警報資料中提取集群 ARN"""
    # 從警報維度中提取集群名稱
    dimensions = alarm_data.get('Trigger', {}).get('Dimensions', [])
    cluster_name = None
    
    for dim in dimensions:
        if dim['name'] == 'Cluster Name':
            cluster_name = dim['value']
            break
    
    if not cluster_name:
        raise ValueError("Could not find cluster name in alarm data")
    
    # 構建集群 ARN
    return f"arn:aws:kafka:{boto3.Session().region_name}:{boto3.client('sts').get_caller_identity()['Account']}:cluster/{cluster_name}"
`),
    environment: {
      CLUSTER_ARN: this.mskCluster.attrArn
    }
  });
}
```

## 📊 監控和日誌配置

### CloudWatch 日誌群組
```typescript
private setupLogging(): void {
  // MSK Broker 日誌
  const brokerLogGroup = new logs.LogGroup(this, 'MSKBrokerLogGroup', {
    logGroupName: '/aws/msk/broker-logs',
    retention: logs.RetentionDays.ONE_WEEK,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });

  // MSK 控制器日誌
  const controllerLogGroup = new logs.LogGroup(this, 'MSKControllerLogGroup', {
    logGroupName: '/aws/msk/controller-logs', 
    retention: logs.RetentionDays.ONE_WEEK,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });

  // 應用程式日誌
  const appLogGroup = new logs.LogGroup(this, 'MSKAppLogGroup', {
    logGroupName: '/aws/msk/application-logs',
    retention: logs.RetentionDays.TWO_WEEKS,
    removalPolicy: cdk.RemovalPolicy.DESTROY
  });
}
```
