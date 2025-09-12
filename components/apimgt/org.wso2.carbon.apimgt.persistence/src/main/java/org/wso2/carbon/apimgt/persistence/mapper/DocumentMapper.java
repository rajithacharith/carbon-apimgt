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
 *
 */
package org.wso2.carbon.apimgt.persistence.mapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.DocumentationContent;
import org.wso2.carbon.apimgt.persistence.dto.DocumentContent;

@Mapper
public interface DocumentMapper {
    Log log = LogFactory.getLog(DocumentMapper.class);
    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);
    
    default Documentation toDocumentation(org.wso2.carbon.apimgt.persistence.dto.Documentation doc) {
        if (doc == null) {
            if (log.isDebugEnabled()) {
                log.debug("Documentation DTO is null, returning null Documentation");
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Converting documentation DTO to API model: " + (doc.getName() != null ? doc.getName() : "unnamed"));
        }
        return INSTANCE.performDocumentationMapping(doc);
    }
    
    Documentation performDocumentationMapping(org.wso2.carbon.apimgt.persistence.dto.Documentation doc);

    default DocumentationContent toDocumentationContent(DocumentContent content) {
        if (content == null) {
            if (log.isDebugEnabled()) {
                log.debug("DocumentContent is null, returning null DocumentationContent");
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Converting document content from persistence DTO to API model");
        }
        return INSTANCE.performDocumentationContentMapping(content);
    }
    
    DocumentationContent performDocumentationContentMapping(DocumentContent content);

    default org.wso2.carbon.apimgt.persistence.dto.Documentation toDocumentation(Documentation documentation) {
        if (documentation == null) {
            if (log.isDebugEnabled()) {
                log.debug("Documentation API model is null, returning null DTO");
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Converting documentation API model to DTO: " + 
                (documentation.getName() != null ? documentation.getName() : "unnamed"));
        }
        return INSTANCE.performDocumentationDTOMapping(documentation);
    }
    
    org.wso2.carbon.apimgt.persistence.dto.Documentation performDocumentationDTOMapping(Documentation documentation);

    default DocumentContent toDocumentContent(DocumentationContent content) {
        if (content == null) {
            if (log.isDebugEnabled()) {
                log.debug("DocumentationContent API model is null, returning null DTO");
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Converting documentation content from API model to persistence DTO");
        }
        return INSTANCE.performDocumentContentDTOMapping(content);
    }
    
    DocumentContent performDocumentContentDTOMapping(DocumentationContent content);
}
