#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "@aws-cdk/core";
import { PipelineStack } from "../lib/pipeline-stack";
import { DomainStack } from "../lib/domain-stack";

const app = new cdk.App();

const pipeline = new PipelineStack(app, "StockMasterPipelineStack", {
  env: { region: "us-east-1" },
});

new DomainStack(app, "StockMasterDomainStack", {
  env: { region: "us-east-1" },
  bucket: pipeline.deploymentBucket,
});
