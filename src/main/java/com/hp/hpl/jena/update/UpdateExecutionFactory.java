/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.update;

import com.hp.hpl.jena.query.ARQ ;
import com.hp.hpl.jena.sparql.modify.UpdateEngineFactory ;
import com.hp.hpl.jena.sparql.modify.UpdateEngineRegistry ;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote ;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemoteForm ;
import com.hp.hpl.jena.sparql.modify.UpdateProcessorBase ;
import com.hp.hpl.jena.sparql.modify.UpdateProcessorStreamingBase ;
import com.hp.hpl.jena.sparql.util.Context ;

/** Create UpdateProcessors (one-time executions of a SPARQL Update request) */
public class UpdateExecutionFactory
{

    /** Create an UpdateProcessor appropriate to the GraphStore, or null if no available factory to make an UpdateProcessor 
     * @param update
     * @param graphStore
     * @return UpdateProcessor or null
     */
    public static UpdateProcessor create(Update update, GraphStore graphStore)
    {
        return create(new UpdateRequest(update), graphStore) ;
    }

    /** Create an UpdateProcessor appropriate to the GraphStore, or null if no available factory to make an UpdateProcessor 
     * @param updateRequest
     * @param graphStore
     * @return UpdateProcessor or null
     */
    public static UpdateProcessor create(UpdateRequest updateRequest, GraphStore graphStore)
    {        
        return make(updateRequest, graphStore, null) ;
    }
    
    /** Create an UpdateProcessor appropriate to the GraphStore, or null if no available factory to make an UpdateProcessor 
     * @param graphStore
     * @return UpdateProcessor or null
     */
    public static UpdateProcessorStreaming createStreaming(GraphStore graphStore)
    {        
        return makeStreaming(graphStore, null) ;
    }
    
    /** Create an UpdateProcessor appropriate to the GraphStore, or null if no available factory to make an UpdateProcessor 
     * @param graphStore
     * @param context  (null means use merge of global and graph store context))
     * @return UpdateProcessor or null
     */
    public static UpdateProcessorStreaming createStreaming(GraphStore graphStore, Context context)
    {        
        return makeStreaming(graphStore, context) ;
    }

    /** Create an UpdateProcessor appropriate to the GraphStore, or null if no available factory to make an UpdateProcessor 
     * @param updateRequest
     * @param graphStore
     * @param context  (null means use merge of global and graph store context))
     * @return UpdateProcessor or null
     */
    public static UpdateProcessor create(UpdateRequest updateRequest, GraphStore graphStore, Context context)
    {        
        return make(updateRequest, graphStore, context) ;
    }

    // Everything comes through one of these two make methods
    private static UpdateProcessor make(UpdateRequest updateRequest, GraphStore graphStore, Context context)
    {
        if ( context == null )
        {
            context = ARQ.getContext().copy();
            context.putAll(graphStore.getContext()) ;
        }
        
        UpdateEngineFactory f = UpdateEngineRegistry.get().find(graphStore, context) ;
        if ( f == null )
            return null ;
        
        UpdateProcessorBase uProc = new UpdateProcessorBase(updateRequest, graphStore, context, f) ;
        return uProc ;
    }
    
    // Everything comes through one of these two make methods
    private static UpdateProcessorStreaming makeStreaming(GraphStore graphStore, Context context)
    {
        if ( context == null )
        {
            context = ARQ.getContext().copy();
            context.putAll(graphStore.getContext()) ;
        }
        
        UpdateEngineFactory f = UpdateEngineRegistry.get().find(graphStore, context) ;
        if ( f == null )
            return null ;
        
        UpdateProcessorStreamingBase uProc = new UpdateProcessorStreamingBase(graphStore, context, f) ;
        return uProc;
    }
    
    
    
    /** Create an UpdateProcessor that send the update to a remote SPARQL Update service.
     * @param update
     * @param remoteEndpoint
     */
    public static UpdateProcessor createRemote(Update update, String remoteEndpoint)
    {
        return createRemote(update, remoteEndpoint, null) ;
    }
    
    /** Create an UpdateProcessor that send the update to a remote SPARQL Update service.
     * @param update
     * @param remoteEndpoint
     * @param context
     */
    public static UpdateProcessor createRemote(Update update, String remoteEndpoint, Context context)
    {
        return createRemote(new UpdateRequest(update), remoteEndpoint, context) ;
    }
    
    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service.
     * @param updateRequest
     * @param remoteEndpoint
     */
    public static UpdateProcessor createRemote(UpdateRequest updateRequest, String remoteEndpoint)
    {
        return new UpdateProcessRemote(updateRequest, remoteEndpoint, null) ;
    }

    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service.
     * @param updateRequest
     * @param remoteEndpoint
     * @param context
     */
    public static UpdateProcessor createRemote(UpdateRequest updateRequest, String remoteEndpoint, Context context)
    {
        return new UpdateProcessRemote(updateRequest, remoteEndpoint, context) ;
    }
    
    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service using an HTML form
     * @param update
     * @param remoteEndpoint
     */
    public static UpdateProcessor createRemoteForm(Update update, String remoteEndpoint)
    {
        return createRemoteForm(update, remoteEndpoint, null) ;
    }
    
    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service using an HTML form
     * @param update
     * @param remoteEndpoint
     * @param context
     */
    public static UpdateProcessor createRemoteForm(Update update, String remoteEndpoint, Context context)
    {
        return new UpdateProcessRemoteForm(new UpdateRequest(update), remoteEndpoint, null) ;
    }
    
    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service using an HTML form
     * @param updateRequest
     * @param remoteEndpoint
     */
    public static UpdateProcessor createRemoteForm(UpdateRequest updateRequest, String remoteEndpoint)
    {
        return createRemoteForm(updateRequest, remoteEndpoint, null) ;
    }
    
    /** Create an UpdateProcessor that send the update request to a remote SPARQL Update service using an HTML form
     * @param updateRequest
     * @param remoteEndpoint
     * @param context
     */
    public static UpdateProcessor createRemoteForm(UpdateRequest updateRequest, String remoteEndpoint, Context context)
    {
        return new UpdateProcessRemoteForm(updateRequest, remoteEndpoint, context) ;
    }
}
