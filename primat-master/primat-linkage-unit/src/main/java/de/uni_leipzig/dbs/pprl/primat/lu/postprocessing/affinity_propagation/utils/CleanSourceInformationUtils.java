/*
 * Copyright © 2016 - 2020 Leipzig University (Database Research Group)
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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;


/**
 * Utility class to provide functions for the handling of clean source information in the
 * MSCD-Affinity-Propagation algorithm.
 */
public class CleanSourceInformationUtils {

  /**
   * Get the source name of the vertex, if the vertex is from a
   * clean source. There are three different possibilities, how a source can be clean:
   * <ol>
   *   <li>all sources are clean ({@link ApConfig#isAllSourcesClean()})</li>
   *   <li>name is defined in clean sources list ({@link ApConfig#getCleanSources()})</li>
   *   <li>there is a vertex property defined, that contains the information whether the source is dirty or
   *       not ({@link ApConfig#getSourceDirtinessVertexProperty()})</li>
   * </ol>
   *
   * @param vertex The vertex whose clean source information shall be returned.
   * @param apConfig The configuration of the Affinity Propagation clustering process.
   *
   * @return The name of the vertex's source, in the case the source is clean. An empty string otherwise.
   */
  public static String getVertexCleanSourceInformation(Record vertex, ApConfig apConfig) {

    String source = vertex.getParty().getName();
    boolean sourceIsClean = false;

    // three ways for the user to define the clean sources per config
    // 1. all sources are clean
    // 2. clean sources list defined - add all vertices from that list
    // 3. vertex property defined that contains the information whether the source is dirty
    // - add the clean ones (dirtinessProperty = false)
    if (!apConfig.getSourceDirtinessVertexProperty().equals("")) {
      sourceIsClean = !Boolean.parseBoolean(vertex.getQidAttribute(
              apConfig.getSourceDirtinessVertexProperty()).getStringValue());
    }

    if (apConfig.isAllSourcesClean() || apConfig.getCleanSources().contains(source) || sourceIsClean) {
      return source;
    } else {
      return "";
    }
  }
}
