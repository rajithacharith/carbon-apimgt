package org.wso2.apk.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel(description = "Meta data of certificate")

public class ClientCertMetadataDTO   {
  
    private String alias = null;
    private String apiId = null;
    private String tier = null;

  /**
   **/
  public ClientCertMetadataDTO alias(String alias) {
    this.alias = alias;
    return this;
  }

  
  @ApiModelProperty(example = "wso2carbon", value = "")
  @JsonProperty("alias")
  public String getAlias() {
    return alias;
  }
  public void setAlias(String alias) {
    this.alias = alias;
  }

  /**
   **/
  public ClientCertMetadataDTO apiId(String apiId) {
    this.apiId = apiId;
    return this;
  }

  
  @ApiModelProperty(example = "64eca60b-2e55-4c38-8603-e9e6bad7d809", value = "")
  @JsonProperty("apiId")
  public String getApiId() {
    return apiId;
  }
  public void setApiId(String apiId) {
    this.apiId = apiId;
  }

  /**
   **/
  public ClientCertMetadataDTO tier(String tier) {
    this.tier = tier;
    return this;
  }

  
  @ApiModelProperty(example = "Gold", value = "")
  @JsonProperty("tier")
  public String getTier() {
    return tier;
  }
  public void setTier(String tier) {
    this.tier = tier;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientCertMetadataDTO clientCertMetadata = (ClientCertMetadataDTO) o;
    return Objects.equals(alias, clientCertMetadata.alias) &&
        Objects.equals(apiId, clientCertMetadata.apiId) &&
        Objects.equals(tier, clientCertMetadata.tier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, apiId, tier);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClientCertMetadataDTO {\n");
    
    sb.append("    alias: ").append(toIndentedString(alias)).append("\n");
    sb.append("    apiId: ").append(toIndentedString(apiId)).append("\n");
    sb.append("    tier: ").append(toIndentedString(tier)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

