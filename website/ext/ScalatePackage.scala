/**
 * Copyright (C) 2009-2010 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
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

import org.fusesource.scalate.support.TemplatePackage
import org.fusesource.scalate.{Binding, TemplateSource}


/**
 * Defines the template package of reusable imports, attributes and methods across templates
 */
class ScalatePackage extends TemplatePackage {
  def header(source: TemplateSource, bindings: List[Binding]) =
    """
    // common imports go here
    import _root_.Website._;
    import java.net._;
    import java.io._;
    import org.fusesource.scalate.util.IOUtil._;
    """
}
