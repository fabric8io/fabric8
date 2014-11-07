/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.cxf.maven;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class AnnotationVisitor extends ASTVisitor {
    
    private boolean hasPath;
    private boolean hasApi;
    private String rootPath;
    private String restServiceClass;
    private List<MethodDeclaration> restMethod = new ArrayList<MethodDeclaration>();
    
    
    @Override
    public boolean visit(NormalAnnotation node) {
        if (node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION && node.getTypeName().toString().equals("Api")) {
            //has swagger annotation
            hasApi = true;
            return false;
        }
        return true;
    }
    
    @Override
    public boolean visit(SingleMemberAnnotation node) {
        if (node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION && node.getTypeName().toString().equals("Path")) {
            //this is a REST serivce class
            hasPath = true;
            this.rootPath = node.getValue().toString();
            this.restServiceClass = ((TypeDeclaration)node.getParent()).getName().toString();
            return false;
        }
        if (node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION && node.getTypeName().toString().equals("Path")) {
            //this is a REST method
            restMethod.add((MethodDeclaration)node.getParent());
            return false;
        }
        return true;
    }
    
    public boolean hasPath() {
        return hasPath;
    }
    
    public boolean hasApi() {
        return hasApi;
    }
    
    public List<MethodDeclaration> getRestMethod() {
        return this.restMethod;
    }
    
    public String getRootPath() {
        return this.rootPath;
    }
  
    public String getRestServiceClass() {
        return this.restServiceClass;
    }
 }
