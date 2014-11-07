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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * @goal addswaggerannotation
 * @description Add swagger annotation from java doc and JAX-RS annotation
 * @threadSafe
 */
public class AddSwaggerAnnotationMojo extends AbstractMojo {

    /**
     * Project the plugin is called from.
     * 
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * Defines files in the source directories to include (all .java files by default).
     * 
     * @parameter
     */
    private String[] includes = {
        "**/*.java"
    };

    /**
     * Defines which of the included files in the source directories to exclude (non by default).
     * 
     * @parameter
     */
    private String[] excludes;

    public void execute() throws MojoExecutionException {
        scan((List<String>)project.getCompileSourceRoots());
    }

    /**
     * Scans a set of directories.
     * 
     * @param roots Directories to scan
     * @throws MojoExecutionException propagated.
     */
    private void scan(List<String> roots) throws MojoExecutionException {
        for (String root : roots) {
            scan(new File(root));
        }
    }

    /**
     * Scans a single directory.
     * 
     * @param root Directory to scan
     * @throws MojoExecutionException in case of IO errors
     */
    private void scan(File root) throws MojoExecutionException {

        if (!root.exists()) {
            return;
        }

        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes(includes);
        directoryScanner.setExcludes(excludes);
        directoryScanner.setBasedir(root);
        directoryScanner.scan();

        for (String fileName : directoryScanner.getIncludedFiles()) {
            final File file = new File(root, fileName);
            try {
                processJavaFile(file);
            } catch (Exception e) {
                throw new MojoExecutionException("io error while rewriting source file", e);
            }
        }
    }

    public void processJavaFile(File file) throws IOException, 
                                                  MalformedTreeException, 
                                                  BadLocationException,
                                                  JavaModelException, 
                                                  IllegalArgumentException {
        String source = FileUtils.readFileToString(file);
        Document document = new Document(source);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setResolveBindings(true);
        parser.setSource(document.get().toCharArray());
        CompilationUnit unit = (CompilationUnit)parser.createAST(null);
        // unit.recordModifications();
        AnnotationVisitor visitor = new AnnotationVisitor();
        unit.accept(visitor);
        if (visitor.hasPath() && !visitor.hasApi()) {
            // this is rest service without swagger annotation so we add it here
            addSwaggerApiAnnotation(unit, visitor, file, document);
        }
    }

    private void addSwaggerApiAnnotation(CompilationUnit unit, AnnotationVisitor visitor, File file,
                                         Document document) 
                                             throws JavaModelException,
                                                    IllegalArgumentException, 
                                                    MalformedTreeException, 
                                                    BadLocationException, 
                                                    IOException {
        AST ast = unit.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        ListRewrite listRewrite = rewriter.getListRewrite(unit, CompilationUnit.TYPES_PROPERTY);
        NormalAnnotation normalAnnotation = rewriter.getAST().newNormalAnnotation();
        Name name = ast.newName("com.wordnik.swagger.annotations.Api");
        normalAnnotation.setTypeName(name);

        MemberValuePair memberValuePair = ast.newMemberValuePair();
        memberValuePair.setName(ast.newSimpleName("value"));
        StringLiteral stringLiteral = ast.newStringLiteral();
        String rootPath = visitor.getRootPath();
        rootPath = rootPath.substring(1, rootPath.length() - 1);
        if (rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }
        stringLiteral.setLiteralValue(rootPath);
        memberValuePair.setValue(stringLiteral);

        normalAnnotation.values().add(memberValuePair);

        memberValuePair = ast.newMemberValuePair();
        memberValuePair.setName(ast.newSimpleName("description"));
        stringLiteral = ast.newStringLiteral();
        stringLiteral.setLiteralValue("Operations about " + visitor.getRestServiceClass());
        memberValuePair.setValue(stringLiteral);

        normalAnnotation.values().add(memberValuePair);

        listRewrite.insertAt(normalAnnotation, 0, null);

        for (MethodDeclaration method : visitor.getRestMethod()) {
            listRewrite = rewriter.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
            normalAnnotation = rewriter.getAST().newNormalAnnotation();
            name = ast.newName("com.wordnik.swagger.annotations.ApiOperation");
            normalAnnotation.setTypeName(name);

            memberValuePair = ast.newMemberValuePair();
            memberValuePair.setName(ast.newSimpleName("value"));
            stringLiteral = ast.newStringLiteral();
            stringLiteral.setLiteralValue(method.getName().toString());
            memberValuePair.setValue(stringLiteral);

            normalAnnotation.values().add(memberValuePair);

            Javadoc doc = method.getJavadoc();
            String comment = null;
            if (doc != null) {
                comment = method.getJavadoc().toString();
            }
            if (comment != null && comment.length() > 0) {
                //add notes from method java doc
                memberValuePair = ast.newMemberValuePair();
                memberValuePair.setName(ast.newSimpleName("notes"));
                stringLiteral = ast.newStringLiteral();
                stringLiteral.setLiteralValue(comment);
                memberValuePair.setValue(stringLiteral);

                normalAnnotation.values().add(memberValuePair);
            }

            listRewrite.insertAt(normalAnnotation, 0, null);

            listRewrite = rewriter
                .getListRewrite((ASTNode)((List)method
                                    .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY)).get(0),
                                SingleVariableDeclaration.MODIFIERS2_PROPERTY);
            normalAnnotation = rewriter.getAST().newNormalAnnotation();
            name = ast.newName("com.wordnik.swagger.annotations.ApiParam");
            normalAnnotation.setTypeName(name);

            ((VariableDeclaration)((List)method.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY))
                .get(0)).getName();
            memberValuePair = ast.newMemberValuePair();
            memberValuePair.setName(ast.newSimpleName("value"));
            stringLiteral = ast.newStringLiteral();
            stringLiteral.setLiteralValue(((VariableDeclaration)((List)method
                .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY)).get(0)).getName().toString());
            memberValuePair.setValue(stringLiteral);

            normalAnnotation.values().add(memberValuePair);
            listRewrite.insertAt(normalAnnotation, 0, null);
        }

        TextEdit edits = rewriter.rewriteAST(document, null);

        edits.apply(document);

        FileUtils.writeStringToFile(file, document.get());
    }

}
