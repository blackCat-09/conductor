/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.conductor.server.resources.v2;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.ApplicationException;
import com.netflix.conductor.metrics.Monitors;
import com.sun.jersey.api.core.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;


/**
 * @author Viren
 *
 */
@Provider
@Singleton
public class GenericExceptionMapper implements ExceptionMapper<ApplicationException> {

    private static Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);

    private static List<Variant> supportedMediaTypes = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE,
            MediaType.TEXT_HTML_TYPE, MediaType.TEXT_PLAIN_TYPE).add().build();

    private static final String LOG_MESSAGE_FORMAT = "%s message= '%s' url= '%s'";

    @Context
    private HttpContext context;

    @Context
    private UriInfo uriInfo;

    @Context
    private javax.inject.Provider<Request> request;

    private String host;

    @Inject
    public GenericExceptionMapper(Configuration config) {
        this.host = config.getServerId();
    }

    @Override
    public Response toResponse(ApplicationException e) {
        logException(e);

        Response.ResponseBuilder responseBuilder = Response.status(e.getHttpStatusCode());

        if(e.getHttpStatusCode() == 500) {
            Monitors.error("error", "error");
        }

        MediaType mediaType = context.getRequest().selectVariant(supportedMediaTypes).getMediaType();

        Map<String, Object> entityMap = e.toMap();
        entityMap.put("instance", host);

        if(mediaType == null){
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        }

        Object entity = entityMap;

        if (mediaType != MediaType.APPLICATION_JSON_TYPE) {
            entity = e.toMap().toString();
        }

        responseBuilder.type(mediaType);
        responseBuilder.entity(entity);

        return responseBuilder.build();
    }

    @VisibleForTesting
    UriInfo getUriInfo() {
        return uriInfo;
    }

    @VisibleForTesting
    Request getRequest() {
        return request.get();
    }

    private void logException(ApplicationException exception) {
        String logMessage = String.format(LOG_MESSAGE_FORMAT, exception.getClass().getSimpleName(),
                exception.getMessage(), getUriInfo().getPath());
        logger.error(logMessage, exception);
    }
}
