package com.aws.ec2.transitgatewayvpcattachment.workflow.read;

import com.aws.ec2.transitgatewayvpcattachment.CallbackContext;
import com.aws.ec2.transitgatewayvpcattachment.Options;
import com.aws.ec2.transitgatewayvpcattachment.ResourceModel;
import com.aws.ec2.transitgatewayvpcattachment.workflow.ExceptionMapper;
import com.aws.ec2.transitgatewayvpcattachment.workflow.TagUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.cloudformation.proxy.*;

public class Read {
    AmazonWebServicesClientProxy proxy;
    ResourceHandlerRequest<ResourceModel> request;
    CallbackContext callbackContext;
    ProxyClient<Ec2Client> client;
    Logger logger;
    ProgressEvent<ResourceModel, CallbackContext>  progress;



    public Read(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, ProxyClient<Ec2Client> client, Logger logger) {

        this.proxy = proxy;
        this.request = request;
        this.callbackContext = callbackContext;
        this.client = client;
        this.logger = logger;
    }


    public ProgressEvent<ResourceModel, CallbackContext>  run(ProgressEvent<ResourceModel, CallbackContext> progress) {
        this.progress = progress;

        return this.proxy.initiate(this.getClass().getSimpleName(), this.client, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(this::translateModelToRequest)
                .makeServiceCall(this::makeServiceCall)
                .handleError(this::handleError)
                .done(this::done);
    }

    public ResourceModel simpleRequest(final ResourceModel model) {
        return this.translateResponseToModel(
                this.proxy.injectCredentialsAndInvokeV2(
                        this.translateModelToRequest(model),
                        this.client.client()::describeTransitGatewayVpcAttachments
                )
        );
    }

    public TransitGatewayAttachmentState stateRequest(final ResourceModel model){
        return this.proxy.injectCredentialsAndInvokeV2(
                this.translateModelToRequest(model),
                this.client.client()::describeTransitGatewayVpcAttachments
        ).transitGatewayVpcAttachments().get(0).state();
    }

    private DescribeTransitGatewayVpcAttachmentsRequest translateModelToRequest(ResourceModel model) {
        return DescribeTransitGatewayVpcAttachmentsRequest.builder()
                .transitGatewayAttachmentIds(model.getId())
                .build();
    }

    private DescribeTransitGatewayVpcAttachmentsResponse makeServiceCall(DescribeTransitGatewayVpcAttachmentsRequest awsRequest, ProxyClient<Ec2Client> client) {
        return this.proxy.injectCredentialsAndInvokeV2(awsRequest, client.client()::describeTransitGatewayVpcAttachments);
    }

    private ResourceModel translateResponseToModel(DescribeTransitGatewayVpcAttachmentsResponse awsResponses) {
        this.logger.log("AWS RESPONSE");
        this.logger.log(awsResponses.toString());
        if(awsResponses.transitGatewayVpcAttachments().isEmpty()) {
            this.logger.log("NO RESPONSE");
            return null;
        }else{
            TransitGatewayVpcAttachment response = awsResponses.transitGatewayVpcAttachments().get(0);
            //System.out.println("this"+response.subnetIds());

            Options options = Options.builder().ipv6Support(response.options().ipv6SupportAsString()).applianceModeSupport(response.options().applianceModeSupportAsString()).dnsSupport(response.options().dnsSupportAsString()).build();
            return ResourceModel.builder()
                    .id(response.transitGatewayAttachmentId())
                    .subnetIds(response.subnetIds())
                    .tags(TagUtils.sdkTagsToCfnTags(response.tags()))
                    .transitGatewayId(response.transitGatewayId())
                    .vpcId(response.vpcId())
                    .options(options)
                    .build();
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext>  handleError(DescribeTransitGatewayVpcAttachmentsRequest awsRequest, Exception exception, ProxyClient<Ec2Client> client, ResourceModel model, CallbackContext context) {
        return ProgressEvent.defaultFailureHandler(exception, ExceptionMapper.mapToHandlerErrorCode(exception));
    }

    private  ProgressEvent<ResourceModel, CallbackContext> done(DescribeTransitGatewayVpcAttachmentsResponse response) {
        return ProgressEvent.defaultSuccessHandler(this.translateResponseToModel(response));
    }

}
