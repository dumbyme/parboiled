/*
 * Copyright (C) 2009 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parboiled;

import org.jetbrains.annotations.NotNull;
import org.parboiled.errors.ParseError;
import org.parboiled.matchers.Matcher;
import org.parboiled.support.InputBuffer;
import org.parboiled.support.MatcherPath;

import java.util.List;

/**
 * A Context object is available to parser actions methods during their runtime and provides various support functionalities.
 *
 * @param <V> the type of the value field of a parse tree node
 */
public interface Context<V> {

    /**
     * Returns the parent context, i.e. the context for the currently running parent matcher.
     *
     * @return the parent context
     */
    Context<V> getParent();

    /**
     * Returns the current sub context, which will only be non-null if the action call leading to this method was
     * wrapped with one or more {@link BaseParser#UP(Object)} calls.
     *
     * @return the current sub context.
     */
    Context<V> getSubContext();

    /**
     * Returns the InputBuffer the parser is currently running against
     *
     * @return the InputBuffer
     */
    @NotNull
    InputBuffer getInputBuffer();

    /**
     * Returns the Matcher of this context or null, if this context is not valid anymore.
     *
     * @return the matcher
     */
    Matcher<V> getMatcher();

    /**
     * Returns the index into the underlying input buffer where the matcher of this context started its match.
     *
     * @return the start index
     */
    int getStartIndex();

    /**
     * Returns the current index in the input buffer.
     *
     * @return the current index
     */
    int getCurrentIndex();

    /**
     * Returns the character at the current index..
     *
     * @return the current character
     */
    char getCurrentChar();

    /**
     * Returns the list of parse errors for the entire parsing run.
     *
     * @return the list of parse errors
     */
    @NotNull
    List<ParseError> getParseErrors();

    /**
     * Returns the input text matched by the given node, with error correction.
     *
     * @param node the node
     * @return null if node is null otherwise a string with the matched input text (which can be empty)
     */
    String getNodeText(Node<V> node);

    /**
     * Returns the {@link MatcherPath} to the currently running matcher.
     *
     * @return the path
     */
    @NotNull
    MatcherPath<V> getPath();

    /**
     * Returns the current matcher level, with 0 being the root level, 1 being one level below the root and so on.
     *
     * @return the current matcher level
     */
    int getLevel();

    /**
     * <p>Returns true if fast string matching is enabled for this parsing run.</p>
     * <p>Fast string matching "short-circuits" the default practice of treating string rules as simple Sequence of
     * character rules. When fast string matching is enabled strings are matched at once, without relying on inner
     * CharacterMatchers. Even though this can lead to significant increases of parsing performance it does not play
     * well with error reporting and recovery, which relies on character level matches.
     * Therefore the {@link ReportingParseRunner} and {@link RecoveringParseRunner} implementations only enable fast
     * string matching during their basic first parsing run and disable it once the input has proven to contain errors.
     * </p>
     *
     * @return true if fast string matching is enabled during the current parsing run
     */
    boolean fastStringMatching();

    /**
     * <p>Returns the first node that matches the given path.
     * See {@link org.parboiled.support.ParseTreeUtils#findNodeByPath(org.parboiled.Node, String)} )} for a
     * description of the path argument.</p>
     * <p><b>Caution</b>: Be aware of the current state of the parse tree when calling this method.
     * You can only reference nodes that have already been created. Since parse tree nodes are immutable they are
     * only created when their complete subtrees are fully determined. This means that all parent nodes have not yet
     * been created when you call this method. Note that you can still access the value objects of future parent
     * nodes through the getParent() chain.</p>
     *
     * @param path the path to the node being searched for
     * @return the node if found or null if not found
     */
    Node<V> getNodeByPath(String path);

    /**
     * <p>Returns the first node that matches the given labelPrefix.
     * <p><b>Caution</b>: Be aware of the current state of the parse tree when calling this method.
     * You can only reference nodes that have already been created. Since parse tree nodes are immutable they are
     * only created when their complete subtrees are fully determined. This means that all parent nodes have not yet
     * been created when you call this method. Note that you can still access the value objects of future parent
     * nodes through the getParent() chain.</p>
     *
     * @param labelPrefix the label prefix to search for
     * @return the node if found or null if not found
     */
    Node<V> getNodeByLabel(String labelPrefix);

    /**
     * Returns the last node created during this parsing run. The last node is independent from the current context
     * scope, i.e. all contexts along the context chain would return the same object at any given point in the
     * parsing process.
     *
     * @return the last node created during this parsing run.
     */
    Node<V> getLastNode();

    /**
     * Sets the value object of the node to be created at the current context scope.
     *
     * @param value the object
     */
    void setNodeValue(V value);

    /**
     * Returns the previously set value of the node to be created at the current context scope.
     *
     * @return the previously set value
     */
    V getNodeValue();

    /**
     * If this context has an explicitly set node value (see {@link #setNodeValue(Object)}) it is returned.
     * Otherwise the method return the last non-null value of the already created subnodes.
     *
     * @return a node value or null
     */
    V getTreeValue();

    /**
     * Returns the parse tree subnodes already created in the current context scope.
     * Note that the returned list is immutable.
     *
     * @return the parse tree subnodes already created in the current context scope
     */
    @NotNull
    List<Node<V>> getSubNodes();

    /**
     * Returns true if the current rule is running somewhere underneath a Test/TestNot rule.
     *
     * @return true if the current context has a parent which corresponds to a Test/TestNot rule
     */
    boolean inPredicate();

    /**
     * Returns true if the current context is for or below a rule marked @SuppressNode or below one
     * marked @SuppressSubnodes.
     *
     * @return true or false
     */
    boolean isNodeSuppressed();

    /**
     * Returns true if this context or any sub node recorded a parse error.
     *
     * @return true if this context or any sub node recorded a parse error
     */
    boolean hasError();

    /**
     * <p>Returns the value object of the context immediately preceeding the action expression that is currently being
     * evaluated. This call can only be used in actions that are part of a Sequence rule and are not at first position
     * in this Sequence.</p>
     * <p>This call is internally delegated to the deepest active Context in the current Context stack, i.e. it can be
     * wrapped by an arbitrary number of UP() / DOWN() wrappers and will always return the same result.</p>
     * <p>This method does not rely on the generated parse tree nodes and can therefore also be used in parts of the
     * grammar where parse tree node creation is suppressed.</p>
     *
     * @return the value object of the immediately preceeding subcontext
     */
    V getPrevValue();

    /**
     * <p>Returns the input text matched by the context immediately preceeding the action expression that is currently
     * being evaluated. This call can only be used in actions that are part of a Sequence rule and are not at first
     * position in this Sequence.</p>
     * <p>This call is internally delegated to the deepest active Context in the current Context stack, i.e. it can be
     * wrapped by an arbitrary number of UP() / DOWN() wrappers and will always return the same result.</p>
     * <p>This method does not rely on the generated parse tree nodes and can therefore also be used in parts of the
     * grammar where parse tree node creation is suppressed.</p>
     *
     * @return the input text matched by the immediately preceeding subcontext
     */
    String getPrevText();

    /**
     * <p>Returns the start index of the context immediately preceeding the action expression that is currently
     * being evaluated. This call can only be used in actions that are part of a Sequence rule and are not at first
     * position in this Sequence.</p>
     * <p>This call is internally delegated to the deepest active Context in the current Context stack, i.e. it can be
     * wrapped by an arbitrary number of UP() / DOWN() wrappers and will always return the same result.</p>
     * <p>This method does not rely on the generated parse tree nodes and can therefore also be used in parts of the
     * grammar where parse tree node creation is suppressed.</p>
     *
     * @return the start index of the context immediately preceeding current action
     */
    int getPrevStartIndex();

    /**
     * <p>Returns the end index of the context immediately preceeding the action expression that is currently
     * being evaluated. This call can only be used in actions that are part of a Sequence rule and are not at first
     * position in this Sequence.</p>
     * <p>This call is internally delegated to the deepest active Context in the current Context stack, i.e. it can be
     * wrapped by an arbitrary number of UP() / DOWN() wrappers and will always return the same result.</p>
     * <p>This method does not rely on the generated parse tree nodes and can therefore also be used in parts of the
     * grammar where parse tree node creation is suppressed.</p>
     *
     * @return the end index of the context immediately preceeding current action, i.e. the index of the character
     *         immediately following the last matched character
     */
    int getPrevEndIndex();

}
