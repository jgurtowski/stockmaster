import * as cdk from "@aws-cdk/core";
import { PublicHostedZone, ARecord, RecordTarget } from "@aws-cdk/aws-route53";
import { BucketWebsiteTarget } from "@aws-cdk/aws-route53-targets";
import { IBucket } from "@aws-cdk/aws-s3";

export interface DomainStackProps extends cdk.StackProps {
  bucket: IBucket;
}

export class DomainStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props: DomainStackProps) {
    super(scope, id, props);
    const hostedZone = new PublicHostedZone(this, "PulsarEquityZone", {
      zoneName: "pulsarequity.com",
    });

    new ARecord(this, "PulsarARecord", {
      zone: hostedZone,
      target: RecordTarget.fromAlias(new BucketWebsiteTarget(props.bucket)),
    });
  }
}
