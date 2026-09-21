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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.exception;

/**
 * Exception class for the case that an algorithm did not converge. This means it did not find a solution
 * in a certain number of iterations.
 */
public class ConvergenceException extends Exception {

  /**
   * Constructs a new convergence exception with the specified detail message.
   *
   * @param message the detail message
   */
  public ConvergenceException(String message) {
    super(message);
  }

  /**
   * Constructs a new convergence exception with the specified detail message and the cause.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public ConvergenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
