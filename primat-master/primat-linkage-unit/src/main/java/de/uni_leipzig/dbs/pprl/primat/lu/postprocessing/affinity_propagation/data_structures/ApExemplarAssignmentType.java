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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures;

/**
 * Type of the exemplar assignment for traditional Affinity Propagation. In the case that no clean sources
 * are configured, MSCD-AP is not used. MSCD-AP always uses CRITERION exemplar assignment. For traditional
 * AP, SIMILARITY assignment most time achieves better results.
 */
public enum ApExemplarAssignmentType {

  /**
   * AP assigns datapoints to exemplars by the highest similarity value.
   */
  SIMILARITY(0),

  /**
   * AP assigns datapoints to exemplars by the highest criterion value.
   */
  CRITERION(1);

  /**
   * Integer value of the enum.
   */
  private final int value;

  /**
   * Constructs the enum by the desired value.
   *
   * @param value integer value of the enum.
   */
  ApExemplarAssignmentType(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of the enum.
   *
   * @return integer value of the enum.
   */
  public int getValue() {
    return value;
  }
}
