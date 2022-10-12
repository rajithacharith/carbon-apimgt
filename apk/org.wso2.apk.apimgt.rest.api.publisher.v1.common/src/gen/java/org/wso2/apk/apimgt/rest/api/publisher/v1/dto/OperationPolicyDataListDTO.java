package org.wso2.apk.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.Valid;



public class OperationPolicyDataListDTO   {
  
    private Integer count = null;
    private List<OperationPolicyDataDTO> list = new ArrayList<OperationPolicyDataDTO>();
    private PaginationDTO pagination = null;

  /**
   * Number of operation policies returned. 
   **/
  public OperationPolicyDataListDTO count(Integer count) {
    this.count = count;
    return this;
  }

  
  @ApiModelProperty(example = "1", value = "Number of operation policies returned. ")
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   **/
  public OperationPolicyDataListDTO list(List<OperationPolicyDataDTO> list) {
    this.list = list;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("list")
  public List<OperationPolicyDataDTO> getList() {
    return list;
  }
  public void setList(List<OperationPolicyDataDTO> list) {
    this.list = list;
  }

  /**
   **/
  public OperationPolicyDataListDTO pagination(PaginationDTO pagination) {
    this.pagination = pagination;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("pagination")
  public PaginationDTO getPagination() {
    return pagination;
  }
  public void setPagination(PaginationDTO pagination) {
    this.pagination = pagination;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationPolicyDataListDTO operationPolicyDataList = (OperationPolicyDataListDTO) o;
    return Objects.equals(count, operationPolicyDataList.count) &&
        Objects.equals(list, operationPolicyDataList.list) &&
        Objects.equals(pagination, operationPolicyDataList.pagination);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, list, pagination);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationPolicyDataListDTO {\n");
    
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    list: ").append(toIndentedString(list)).append("\n");
    sb.append("    pagination: ").append(toIndentedString(pagination)).append("\n");
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

