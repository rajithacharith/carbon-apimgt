/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.persistence.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.wso2.carbon.apimgt.api.model.APICategory;
import org.wso2.carbon.apimgt.api.model.APIProduct;
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.persistence.dto.PublisherAPIProduct;

import com.google.gson.Gson;

@Mapper
public interface APIProductMapper {
    Log log = LogFactory.getLog(APIProductMapper.class);
    APIProductMapper INSTANCE = Mappers.getMapper(APIProductMapper.class);
    
    //@Mapping(source = "providerName", target = "id.providerName")
    //@Mapping(source = "apiProductName", target = "id.apiProductName")
    //@Mapping(source = "version", target = "id.version")
    @Mapping(source = "id", target = "uuid")
    @Mapping(source = "thumbnail", target = "thumbnailUrl")
    @Mapping(source = "availableTierNames", target = "availableTiers")
    @Mapping(source = "visibleOrganizations", target = "visibleTenants")
    @Mapping(source = "subscriptionAvailableOrgs", target = "subscriptionAvailableTenants")
    APIProduct toApiProduct(PublisherAPIProduct product);

    //@Mapping(source = "id.apiProductName", target = "apiProductName")
    //@Mapping(source = "id.version", target = "version")
    @Mapping(source = "id.providerName", target = "providerName")
    @Mapping(source = "id.version", target = "version")
    @Mapping(source = "thumbnailUrl", target = "thumbnail")
    @Mapping(source = "availableTiers", target = "availableTierNames")
    @Mapping(source = "uuid", target = "id")
    @Mapping(source = "visibleTenants", target = "visibleOrganizations")
    @Mapping(source = "subscriptionAvailableTenants", target = "subscriptionAvailableOrgs")
    PublisherAPIProduct toPublisherApiProduct(APIProduct product);
    
    default JSONObject mapJSONMapToJSONObject(Map<String,String> jsonMap) throws ParseException {
        if (jsonMap != null) {
            if (log.isDebugEnabled()) {
                log.debug("Converting API product JSON map to JSONObject with " + jsonMap.size() + " entries");
            }
            try {
                JSONParser parser = new JSONParser();
                String jsonText = JSONValue.toJSONString(jsonMap);
                JSONObject jsonObject = (JSONObject) parser.parse(jsonText);
                return jsonObject;
            } catch (ParseException e) {
                log.error("Failed to parse API product JSON map to JSONObject: " + e.getMessage());
                throw e;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("API product JSON map is null, returning null JSONObject");
        }
        return null;
    }

    default Map<String, Object> JSONObjectToJSONMap(JSONObject jsonObject){
        Gson gson = new Gson();
        if (jsonObject != null) {
            if (log.isDebugEnabled()) {
                log.debug("Converting API product JSONObject to map");
            }
            try {
                jsonObject.toJSONString();
                Map<String,Object> fromJson = gson.fromJson(jsonObject.toJSONString(), Map.class);
                return fromJson;
            } catch (Exception e) {
                log.error("Failed to convert API product JSONObject to map: " + e.getMessage());
                return null;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("API product JSONObject is null, returning null map");
        }
        return null;
    }

    default List<APICategory> mapAPICategoriesToList(Set<String> apiCategories) {
        List<APICategory> categoryList = new ArrayList<APICategory>();
        if (apiCategories != null) {
            for (String category : apiCategories) {
                APICategory apiCategory = new APICategory();
                apiCategory.setName(category);
                categoryList.add(apiCategory);
            }
        }
        return categoryList;
    }
    default Set<String> mapAPICategoriesToSet(List<APICategory> apiCategories){
        Set<String> categorySet = new HashSet<String>();
        if (apiCategories != null) {
            for (APICategory category : apiCategories) {
                categorySet.add(category.getName());
            }
        }
        return categorySet;

    }

    /*
     * this method is created to create compatibility with json-simple 1.1
     */
    default JSONObject mapJson(JSONObject json) {
        return json;
    }

    default Date mapStringToDate(String dateString) {
        if (dateString != null) {
            try {
                return new Date(Long.parseLong(dateString));
            } catch (NumberFormatException e) {
                log.error("Failed to parse date string for API product: " + dateString + ". " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    default String mapDateToString(Date date) {
        if (date != null) {
            return Long.toString(date.getTime());
        }
        return null;
    }

    default Set<Tier> mapStringToSet(Set<String> tierSet) {

        HashSet<Tier> mappedTiers = new HashSet<Tier>();
        if (tierSet != null) {
            for (String tierName : tierSet) {
                mappedTiers.add(new Tier(tierName));
            }
        }
        return mappedTiers;
    }

    default Set<String> mapTierToSet(Set<Tier> tierSet) {

        HashSet<String> mappedTiers = new HashSet<String>();
        if (tierSet != null) {
            for (Tier tier : tierSet) {
                mappedTiers.add(tier.getName());
            }
        }
        return mappedTiers;
    }
    default String mapAccessControlRolesToString(Set<String> accessControlRoles) {
        if (accessControlRoles != null) {
            return String.join(",", accessControlRoles);
        } else {
            return null;
        }
    }

    default Set<String> mapAccessControlRolesToSet(String accessControlRoles){
        if (accessControlRoles != null && !"null".equalsIgnoreCase(accessControlRoles)) {
            if (log.isDebugEnabled()) {
                log.debug("Mapping API product access control roles from string: " + accessControlRoles);
            }
            return  new HashSet<>(Arrays.asList(accessControlRoles.split(",")));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("API product access control roles string is null or 'null', returning null set");
            }
            return null;
        }
    }
}
