// GenBI Text-to-SQL 智能查詢系統 CDK Construct
// 創建日期: 2025年9月23日 下午4:22 (台北時間)
// 架構: Angular 前端 → API Gateway → Lambda (Bedrock) → SQL 生成 → Aurora/Athena → 結果處理

import * as cdk from 'aws-cdk-lib';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as bedrock from 'aws-cdk-lib/aws-bedrock';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface GenBITextToSQLProps {
  readonly environment: string;
  readonly projectName: string;
  readonly vpc: ec2.IVpc;
  readonly auroraClusterEndpoint: string;
  readonly athenaWorkgroup: string;
  readonly s3DataBucket: s3.IBucket;
  readonly elastiCacheEndpoint: string;
}

export class GenBITextToSQLConstruct extends Construct {
  public readonly api: apigateway.RestApi;
  public readonly textToSqlLambda: lambda.Function;
  public readonly queryExecutorLambda: lambda.Function;
  public readonly resultProcessorLambda: lambda.Function;
  public readonly queryHistoryTable: dynamodb.Table;

  constructor(scope: Construct, id: string, props: GenBITextToSQLProps) {
    super(scope, id);

    const {
      environment,
      projectName,
      vpc,
      auroraClusterEndpoint,
      athenaWorkgroup,
      s3DataBucket,
      elastiCacheEndpoint
    } = props;

    // 1. 創建 DynamoDB 表存儲查詢歷史和學習數據
    this.queryHistoryTable = this.createQueryHistoryTable(projectName, environment);

    // 2. 創建 Lambda 函數
    this.textToSqlLambda = this.createTextToSqlLambda(projectName, environment, vpc);
    this.queryExecutorLambda = this.createQueryExecutorLambda(projectName, environment, vpc, auroraClusterEndpoint, athenaWorkgroup);
    this.resultProcessorLambda = this.createResultProcessorLambda(projectName, environment, vpc, elastiCacheEndpoint);

    // 3. 配置 IAM 權限
    this.configureLambdaPermissions(props);

    // 4. 創建 API Gateway
    this.api = this.createApiGateway(projectName, environment);

    // 5. 配置 API 路由
    this.configureApiRoutes();
  }

  private createQueryHistoryTable(projectName: string, environment: string): dynamodb.Table {
    return new dynamodb.Table(this, 'QueryHistoryTable', {
      tableName: `${projectName}-${environment}-genbi-query-history`,
      partitionKey: {
        name: 'queryId',
        type: dynamodb.AttributeType.STRING
      },
      sortKey: {
        name: 'timestamp',
        type: dynamodb.AttributeType.NUMBER
      },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      pointInTimeRecovery: true,
      encryption: dynamodb.TableEncryption.AWS_MANAGED,
      removalPolicy: environment === 'production' 
        ? cdk.RemovalPolicy.RETAIN 
        : cdk.RemovalPolicy.DESTROY,
      
      // GSI for user queries
      globalSecondaryIndexes: [{
        indexName: 'UserQueryIndex',
        partitionKey: {
          name: 'userId',
          type: dynamodb.AttributeType.STRING
        },
        sortKey: {
          name: 'timestamp',
          type: dynamodb.AttributeType.NUMBER
        }
      }, {
        indexName: 'QueryPatternIndex',
        partitionKey: {
          name: 'queryPattern',
          type: dynamodb.AttributeType.STRING
        },
        sortKey: {
          name: 'successRate',
          type: dynamodb.AttributeType.NUMBER
        }
      }]
    });
  }

  private createTextToSqlLambda(projectName: string, environment: string, vpc: ec2.IVpc): lambda.Function {
    return new lambda.Function(this, 'TextToSqlLambda', {
      functionName: `${projectName}-${environment}-genbi-text-to-sql`,
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'text_to_sql.handler',
      code: lambda.Code.fromAsset('lambda/genbi-text-to-sql'),
      timeout: cdk.Duration.minutes(5),
      memorySize: 1024,
      vpc: vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
      },
      environment: {
        ENVIRONMENT: environment,
        PROJECT_NAME: projectName,
        QUERY_HISTORY_TABLE: this.queryHistoryTable.tableName,
        BEDROCK_MODEL_ID: 'anthropic.claude-3-5-sonnet-20241022-v2:0',
        BEDROCK_REGION: cdk.Stack.of(this).region,
        LOG_LEVEL: environment === 'production' ? 'INFO' : 'DEBUG'
      },
      logRetention: logs.RetentionDays.ONE_WEEK,
      tracing: lambda.Tracing.ACTIVE,
      insightsVersion: lambda.LambdaInsightsVersion.VERSION_1_0_229_0
    });
  }

  private createQueryExecutorLambda(
    projectName: string, 
    environment: string, 
    vpc: ec2.IVpc,
    auroraEndpoint: string,
    athenaWorkgroup: string
  ): lambda.Function {
    return new lambda.Function(this, 'QueryExecutorLambda', {
      functionName: `${projectName}-${environment}-genbi-query-executor`,
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'query_executor.handler',
      code: lambda.Code.fromAsset('lambda/genbi-query-executor'),
      timeout: cdk.Duration.minutes(10),
      memorySize: 2048,
      vpc: vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
      },
      environment: {
        ENVIRONMENT: environment,
        PROJECT_NAME: projectName,
        AURORA_ENDPOINT: auroraEndpoint,
        ATHENA_WORKGROUP: athenaWorkgroup,
        QUERY_HISTORY_TABLE: this.queryHistoryTable.tableName,
        MAX_QUERY_TIMEOUT: '300', // 5 minutes
        MAX_RESULT_ROWS: '10000',
        LOG_LEVEL: environment === 'production' ? 'INFO' : 'DEBUG'
      },
      logRetention: logs.RetentionDays.ONE_WEEK,
      tracing: lambda.Tracing.ACTIVE,
      insightsVersion: lambda.LambdaInsightsVersion.VERSION_1_0_229_0
    });
  }

  private createResultProcessorLambda(
    projectName: string, 
    environment: string, 
    vpc: ec2.IVpc,
    elastiCacheEndpoint: string
  ): lambda.Function {
    return new lambda.Function(this, 'ResultProcessorLambda', {
      functionName: `${projectName}-${environment}-genbi-result-processor`,
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'result_processor.handler',
      code: lambda.Code.fromAsset('lambda/genbi-result-processor'),
      timeout: cdk.Duration.minutes(3),
      memorySize: 1024,
      vpc: vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
      },
      environment: {
        ENVIRONMENT: environment,
        PROJECT_NAME: projectName,
        REDIS_ENDPOINT: elastiCacheEndpoint,
        QUERY_HISTORY_TABLE: this.queryHistoryTable.tableName,
        CACHE_TTL: '3600', // 1 hour
        LOG_LEVEL: environment === 'production' ? 'INFO' : 'DEBUG'
      },
      logRetention: logs.RetentionDays.ONE_WEEK,
      tracing: lambda.Tracing.ACTIVE,
      insightsVersion: lambda.LambdaInsightsVersion.VERSION_1_0_229_0
    });
  }

  private configureLambdaPermissions(props: GenBITextToSQLProps): void {
    // Bedrock 權限
    const bedrockPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'bedrock:InvokeModel',
        'bedrock:InvokeModelWithResponseStream'
      ],
      resources: [
        `arn:aws:bedrock:${cdk.Stack.of(this).region}::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0`
      ]
    });

    // Aurora 權限
    const auroraPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'rds-data:ExecuteStatement',
        'rds-data:BatchExecuteStatement',
        'rds-data:BeginTransaction',
        'rds-data:CommitTransaction',
        'rds-data:RollbackTransaction'
      ],
      resources: [`arn:aws:rds:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:cluster:*`]
    });

    // Athena 權限
    const athenaPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'athena:StartQueryExecution',
        'athena:GetQueryExecution',
        'athena:GetQueryResults',
        'athena:StopQueryExecution'
      ],
      resources: [
        `arn:aws:athena:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:workgroup/${props.athenaWorkgroup}`
      ]
    });

    // S3 權限
    const s3Policy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        's3:GetObject',
        's3:PutObject',
        's3:DeleteObject',
        's3:ListBucket'
      ],
      resources: [
        props.s3DataBucket.bucketArn,
        `${props.s3DataBucket.bucketArn}/*`
      ]
    });

    // DynamoDB 權限
    const dynamoPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'dynamodb:GetItem',
        'dynamodb:PutItem',
        'dynamodb:UpdateItem',
        'dynamodb:DeleteItem',
        'dynamodb:Query',
        'dynamodb:Scan'
      ],
      resources: [
        this.queryHistoryTable.tableArn,
        `${this.queryHistoryTable.tableArn}/index/*`
      ]
    });

    // 應用權限到所有 Lambda 函數
    [this.textToSqlLambda, this.queryExecutorLambda, this.resultProcessorLambda].forEach(func => {
      func.addToRolePolicy(bedrockPolicy);
      func.addToRolePolicy(auroraPolicy);
      func.addToRolePolicy(athenaPolicy);
      func.addToRolePolicy(s3Policy);
      func.addToRolePolicy(dynamoPolicy);
    });
  }

  private createApiGateway(projectName: string, environment: string): apigateway.RestApi {
    return new apigateway.RestApi(this, 'GenBIApi', {
      restApiName: `${projectName}-${environment}-genbi-api`,
      description: `GenBI Text-to-SQL API for ${projectName} ${environment}`,
      deployOptions: {
        stageName: environment,
        tracingEnabled: true,
        dataTraceEnabled: true,
        loggingLevel: apigateway.MethodLoggingLevel.INFO,
        metricsEnabled: true
      },
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
        allowHeaders: ['Content-Type', 'X-Amz-Date', 'Authorization', 'X-Api-Key', 'X-Amz-Security-Token']
      },
      policy: new iam.PolicyDocument({
        statements: [
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            principals: [new iam.AnyPrincipal()],
            actions: ['execute-api:Invoke'],
            resources: ['*']
          })
        ]
      })
    });
  }

  private configureApiRoutes(): void {
    // /genbi 根路徑
    const genbiResource = this.api.root.addResource('genbi');

    // POST /genbi/query - 自然語言查詢
    const queryResource = genbiResource.addResource('query');
    queryResource.addMethod('POST', new apigateway.LambdaIntegration(this.textToSqlLambda), {
      requestValidator: new apigateway.RequestValidator(this, 'QueryRequestValidator', {
        restApi: this.api,
        validateRequestBody: true,
        requestValidatorName: 'genbi-query-validator'
      }),
      requestModels: {
        'application/json': this.createQueryRequestModel()
      }
    });

    // GET /genbi/history/{userId} - 查詢歷史
    const historyResource = genbiResource.addResource('history');
    const userHistoryResource = historyResource.addResource('{userId}');
    userHistoryResource.addMethod('GET', new apigateway.LambdaIntegration(this.resultProcessorLambda));

    // GET /genbi/templates - 查詢模板
    const templatesResource = genbiResource.addResource('templates');
    templatesResource.addMethod('GET', new apigateway.LambdaIntegration(this.resultProcessorLambda));

    // POST /genbi/feedback - 查詢反饋
    const feedbackResource = genbiResource.addResource('feedback');
    feedbackResource.addMethod('POST', new apigateway.LambdaIntegration(this.resultProcessorLambda));
  }

  private createQueryRequestModel(): apigateway.Model {
    return new apigateway.Model(this, 'QueryRequestModel', {
      restApi: this.api,
      modelName: 'GenBIQueryRequest',
      contentType: 'application/json',
      schema: {
        type: apigateway.JsonSchemaType.OBJECT,
        properties: {
          query: {
            type: apigateway.JsonSchemaType.STRING,
            minLength: 5,
            maxLength: 1000
          },
          userId: {
            type: apigateway.JsonSchemaType.STRING,
            pattern: '^[a-zA-Z0-9-_]+$'
          },
          context: {
            type: apigateway.JsonSchemaType.OBJECT,
            properties: {
              membershipLevel: {
                type: apigateway.JsonSchemaType.STRING,
                enum: ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM']
              },
              timeRange: {
                type: apigateway.JsonSchemaType.STRING,
                enum: ['last_7_days', 'last_30_days', 'last_90_days', 'last_year']
              },
              dataSource: {
                type: apigateway.JsonSchemaType.STRING,
                enum: ['aurora', 'matomo', 'both']
              }
            }
          }
        },
        required: ['query', 'userId']
      }
    });
  }
}