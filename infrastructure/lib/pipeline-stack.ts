import * as cdk from "@aws-cdk/core";
import { User, PolicyStatement, Effect, ManagedPolicy } from "@aws-cdk/aws-iam";
import { Pipeline, Artifact } from "@aws-cdk/aws-codepipeline";
import { Repository } from "@aws-cdk/aws-codecommit";
import { PipelineProject } from "@aws-cdk/aws-codebuild";
import { IBucket, Bucket, BlockPublicAccess } from "@aws-cdk/aws-s3";
import {
  CodeBuildAction,
  CodeCommitSourceAction,
  S3DeployAction,
} from "@aws-cdk/aws-codepipeline-actions";

export class PipelineStack extends cdk.Stack {
  deploymentBucket: IBucket;

  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const jamesUser = User.fromUserName(this, "JamesUser", "james");

    const repo = new Repository(this, "StockMasterRepository", {
      repositoryName: "StockMaster",
      description: "The Tool For Stock Masters",
    });

    new ManagedPolicy(this, "CodeCommitStockMasterPolicy", {
      statements: [
        new PolicyStatement({
          effect: Effect.ALLOW,
          actions: ["codecommit:GitPull", "codecommit:GitPush"],
          resources: [repo.repositoryArn],
        }),
      ],
      users: [jamesUser],
    });

    this.deploymentBucket = new Bucket(this, "StockMasterBucket", {
      bucketName: "pulsarequity.com",
      websiteIndexDocument: "index.html",
      publicReadAccess: true,
      blockPublicAccess: new BlockPublicAccess({
        blockPublicAcls: false,
        blockPublicPolicy: false,
        ignorePublicAcls: false,
        restrictPublicBuckets: false,
      }),
    });

    const pipeline = new Pipeline(this, "StockMasterBuildPipeline");

    const sourceArtifact = new Artifact();

    pipeline.addStage({
      stageName: "Source",
      actions: [
        new CodeCommitSourceAction({
          actionName: "SourcePuller",
          repository: repo,
          output: sourceArtifact,
        }),
      ],
    });

    const pipelineProject = new PipelineProject(
      this,
      "StockMasterPipelineProject"
    );

    const buildArtifact = new Artifact();

    pipeline.addStage({
      stageName: "Build",
      actions: [
        new CodeBuildAction({
          actionName: "BuildAction",
          input: sourceArtifact,
          project: pipelineProject,
          outputs: [buildArtifact],
        }),
      ],
    });

    pipeline.addStage({
      stageName: "Deploy",
      actions: [
        new S3DeployAction({
          actionName: "DeploymentAction",
          bucket: this.deploymentBucket,
          input: buildArtifact,
        }),
      ],
    });
  }
}
