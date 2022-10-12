package org.wso2.apk.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;
import javax.validation.constraints.NotNull;

public class CustomAttributeDTO   {
  
    private String name = null;
    private String value = null;

  /**
   * Name of the custom attribute
   **/
  public CustomAttributeDTO name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(example = "customAttr1", required = true, value = "Name of the custom attribute")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Value of the custom attribute
   **/
  public CustomAttributeDTO value(String value) {
    this.value = value;
    return this;
  }

  
  @ApiModelProperty(example = "value1", required = true, value = "Value of the custom attribute")
  @JsonProperty("value")
  @NotNull
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CustomAttributeDTO customAttribute = (CustomAttributeDTO) o;
    return Objects.equals(name, customAttribute.name) &&
        Objects.equals(value, customAttribute.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomAttributeDTO {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

