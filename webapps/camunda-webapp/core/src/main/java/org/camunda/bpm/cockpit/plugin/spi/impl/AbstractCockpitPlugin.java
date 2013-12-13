/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.cockpit.plugin.spi.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.camunda.bpm.cockpit.plugin.spi.CockpitPlugin;

/**
 * An implementation of {@link CockpitPlugin} that comes with reasonable defaults.
 *
 * @author nico.rehwaldt
 *
 * @see CockpitPlugin
 */
public abstract class AbstractCockpitPlugin implements CockpitPlugin {

  /**
   * Returns a uri to a plugin resources directory.
   * The directory must be unique across all plugins.
   *
   * <p>
   *
   * This implementation assumes that the resources are provided in the directory <code>plugin-webapp/PLUGIN_ID</code>,
   * absolute to the root directory.
   *
   * @return the directory providing the plugins client side resources
   */
  @Override
  public String getAssetDirectory() {
    return String.format("plugin-webapp/%s", getId());
  }

  @Override
  public Set<Class<?>> getResourceClasses() {
    return Collections.EMPTY_SET;
  }

  @Override
  public List<String> getMappingFiles() {
    return Collections.EMPTY_LIST;
  }
}
