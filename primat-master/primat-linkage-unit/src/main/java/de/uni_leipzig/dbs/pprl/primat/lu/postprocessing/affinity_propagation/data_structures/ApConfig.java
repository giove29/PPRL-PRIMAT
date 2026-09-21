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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.exception.ConvergenceException;

/**
 * Configuration class for Affinity Propagation (AP) and its extension Multi-Source Clean-Dirty AP (MSCD-AP).
 */
public class ApConfig implements Serializable {

  /**
   * Constant minimum value for the {@link #dampingFactor}. A damping of less then 50% is to weak and
   * doesn't make much sense.
   */
  public static final double DAMPING_MIN_VALUE = 0.5;

  /**
   * Number of maximum overall iterations for the AP algorithm to find exemplars. This
   * includes all iterations of all parameter adaptions (see {@link ApConfig#maxAdaptionIteration}).
   */
  private int maxApIteration;

  /**
   * Number of maximum iterations for AP until the parameter values
   * ({@link ApConfig#preferenceConfig} and {@link ApConfig#dampingFactor}) are adapted and the clustering
   * is restarted with the new parameters.
   */
  private int maxAdaptionIteration;

  /**
   * Number of iterations that the found exemplars of AP need to stay unchanged, so that the result is
   * accepted as a solution and the algorithm converged.
   */
  private int convergenceIter;

  /**
   * During the iterative process of AP, the message-values A and R get damped, by a certain percentage of
   * their value from the last iteration. This percentage is defined here. The dampingFactor must be a
   * value between 0 and 1.
   */
  private double dampingFactor;

  /**
   * When the dampingFactor of a connected component must be adapted, because AP could not converge for a
   * solution, it gets lowered or raised by this step.
   */
  private double dampingAdaptionStep;

  /**
   * When all similarities between the nodes of a connected component are equal, AP struggles to find a
   * clustering solution for it. So the component converges immediately. If the similarities are greater or
   * equal to the threshold, then the nodes form one big cluster. Clean source dataPoints are still
   * separated into different clusters. Otherwise each node forms a singleton.
   */
  private double allSameSimClusteringThreshold;

  /**
   * AP can oscillate between different solutions, if they are equally well optimizing the energy function
   * of the clustering problem. This happens especially for symmetrical similarities. Noise helps to
   * prevent these oscillations. The noise level can be defined by the decimal position, where most of the
   * noise is added to. The noise is a gaussian random number that is multiplied by 10^(-1 *
   * (noiseDecimalPlace -1)).
   * If noiseDecimalPlace < 0, then noise is disabled.
   */
  private int noiseDecimalPlace;

  /**
   * AP can oscillate between different solutions, if they are equally well optimizing the energy function
   * of the clustering problem. Noise, damping and parameter adaption try to solve this problem. But
   * sometimes a converging solution for a connected component can't be found. If the parameter is set to
   * false, MSCD-AP fails and throws a {@link ConvergenceException}. The results of all other components
   * are lost. If the parameter is set to true, all data points of the failed component become singletons
   * and the clustering of the graph can be finished.
   */
  private boolean singletonsForUnconvergedComponents;

  /**
   * For the MSCD-AP extension, this parameter defines, whether all sources of the dataset are duplicate free.
   */
  private boolean allSourcesClean;

  /**
   * For the MSCD-AP extension, this parameter defines the name of a vertex property, that contains a
   * boolean flag, whether the source of the vertex contains duplicates (true) or not (false).
   */
  private String sourceDirtinessVertexProperty;

  /**
   * For the MSCD-AP extension, this parameter defines a list of the names of all duplicate free sources in
   * the dataset.
   */
  private List<String> cleanSources;

  /**
   * The preference is the most important parameter of AP. There are many different parameter tuning
   * possibilities. That's why there is a special configuration class for this parameter.
   */
  private PreferenceConfig preferenceConfig;

  /**
   * Type of the exemplar assignment for traditional Affinity Propagation. In the case that no clean sources
   * are configured, MSCD-AP is not used. MSCD-AP always uses CRITERION exemplar assignment. For traditional
   * AP, SIMILARITY assignment most time achieves better results.
   */
  private ApExemplarAssignmentType apExemplarAssignmentType;

  /**
   * Contains the type of AP that runs the clustering. This field is just used for evaluation purposes.
   */
  private ApType apType;

  /**
   * Creates an instance of ApConfig with default values.
   */
  public ApConfig() {
    this.maxApIteration = 20000;
    this.maxAdaptionIteration = 150;
    this.convergenceIter = 15;
    this.dampingFactor = 0.5;
    this.dampingAdaptionStep = 0.1;
    this.allSameSimClusteringThreshold = 0.3;
    this.noiseDecimalPlace = 3;
    this.singletonsForUnconvergedComponents = false;
    this.allSourcesClean = false;
    this.sourceDirtinessVertexProperty = "";
    this.apExemplarAssignmentType = ApExemplarAssignmentType.SIMILARITY;
    this.cleanSources = new ArrayList<>();
    this.preferenceConfig = new PreferenceConfig();
  }

  /**
   * Checks if all parameter values are in an allowed value range.
   *
   * @throws IllegalArgumentException if a parameter value is outside of the allowed range.
   */
  public void checkConfigCorrectness() throws IllegalArgumentException {

    if (maxApIteration <= 0) {
      throw new IllegalArgumentException("maxApIteration must be greater than 0");
    }

    if (maxApIteration < maxAdaptionIteration) {
      throw new IllegalArgumentException("maxApIteration must be greater or equal to maxAdaptionIteration");
    }

    if (maxAdaptionIteration <= 0) {
      throw new IllegalArgumentException("maxAdaptionIteration must be greater than 0");
    }

    if (maxAdaptionIteration < convergenceIter) {
      throw new IllegalArgumentException("maxAdaptionIteration must be greater or equal to convergenceIter");
    }

    if (convergenceIter < 2) {
      throw new IllegalArgumentException("convergenceIter must be greater than 1");
    }

    if (dampingFactor < DAMPING_MIN_VALUE || dampingFactor >= 1) {
      throw new IllegalArgumentException(String.format(
        "dampingFactor must be a value in the interval [%.1f, 1)", DAMPING_MIN_VALUE));
    }

    if (dampingAdaptionStep < 0 || dampingAdaptionStep >= 1) {
      throw new IllegalArgumentException("dampingAdaptionStep must be a value between 0 and 1");
    }

    if (allSameSimClusteringThreshold < 0 || allSameSimClusteringThreshold >= 1) {
      throw new IllegalArgumentException("allSameSimClusteringThreshold must be a value between 0 and 1");
    }

    if (cleanSources == null) {
      throw new IllegalArgumentException(
          "cleanSources list is not allowed to be null. Use an empty list instead");
    }

    if (sourceDirtinessVertexProperty == null) {
      throw new IllegalArgumentException(
        "sourceDirtinessVertexProperty is not allowed to be null. Use an empty String instead");
    }

    preferenceConfig.checkConfigCorrectness();
  }

  /**
   * Create a name for the config that contains the most important parameter values. This name can be used
   * as a file name for evaluation results.
   *
   * @return String with important parameters and values.
   */
  public String getConfigName() {

    int cs = isCleanSourceExtensionActivated() ? 1 : 0;

    String dirtyPref;
    String cleanPref;

    if (preferenceConfig.isPreferenceUseMinSimilarityDirtySrc()) {
      dirtyPref = "d_pref_min";
    } else {
      if (preferenceConfig.getPreferenceFixValueDirtySrc() > 0) {
        dirtyPref = String.format(Locale.US, "d_fixpref_%.2f",
          preferenceConfig.getPreferenceFixValueDirtySrc());
      } else {
        dirtyPref = String.format(Locale.US, "d_pref_%d",
          preferenceConfig.getPreferencePercentileDirtySrc());
      }
    }

    if (preferenceConfig.isPreferenceUseMinSimilarityCleanSrc()) {
      cleanPref = "c_pref_min";
    } else {
      if (preferenceConfig.getPreferenceFixValueCleanSrc() > 0) {
        cleanPref = String.format(Locale.US, "c_fixpref_%.2f",
          preferenceConfig.getPreferenceFixValueCleanSrc());
      } else {
        cleanPref = String.format(Locale.US, "c_pref_%d",
          preferenceConfig.getPreferencePercentileCleanSrc());
      }
    }

    return String.format(Locale.US, "dmp_%.2f__%s__%s__noise_%d__cs_%d__apt_%d",
      dampingFactor, dirtyPref, cleanPref, noiseDecimalPlace, cs, apType.getValue());
  }

  /**
   * Checks the parameters that define the clean sources for the MSCD-AP extension to determine, whether
   * the extension is activated or not.
   * @return true, if at least one clean source parameter is set.
   */
  public boolean isCleanSourceExtensionActivated() {

    boolean result = false;
    if (!sourceDirtinessVertexProperty.equals("") || allSourcesClean || cleanSources.size() > 0) {
      result = true;
    }
    return result;
  }

  /**
   * Add a clean source to {@link #cleanSources}.
   *
   * @param cleanSource name of the clean source to add
   * @return true, if the collection {@link #cleanSources has changed}
   */
  public boolean addCleanSource(String cleanSource) {
    return this.cleanSources.add(cleanSource);
  }

  public int getMaxApIteration() {
    return maxApIteration;
  }

  public void setMaxApIteration(int maxApIteration) {
    this.maxApIteration = maxApIteration;
  }

  public int getMaxAdaptionIteration() {
    return maxAdaptionIteration;
  }

  public void setMaxAdaptionIteration(int maxAdaptionIteration) {
    this.maxAdaptionIteration = maxAdaptionIteration;
  }

  public double getDampingFactor() {
    return dampingFactor;
  }

  public void setDampingFactor(double dampingFactor) {
    this.dampingFactor = dampingFactor;
  }

  public double getDampingAdaptionStep() {
    return dampingAdaptionStep;
  }

  public void setDampingAdaptionStep(double dampingAdaptionStep) {
    this.dampingAdaptionStep = dampingAdaptionStep;
  }

  public PreferenceConfig getPreferenceConfig() {
    return preferenceConfig;
  }

  public void setPreferenceConfig(PreferenceConfig preferenceConfig) {
    this.preferenceConfig = preferenceConfig;
  }

  public int getConvergenceIter() {
    return convergenceIter;
  }

  public void setConvergenceIter(int convergenceIter) {
    this.convergenceIter = convergenceIter;
  }

  public double getAllSameSimClusteringThreshold() {
    return allSameSimClusteringThreshold;
  }

  public void setAllSameSimClusteringThreshold(double allSameSimClusteringThreshold) {
    this.allSameSimClusteringThreshold = allSameSimClusteringThreshold;
  }

  public int getNoiseDecimalPlace() {
    return noiseDecimalPlace;
  }

  public void setNoiseDecimalPlace(int noiseDecimalPlace) {
    this.noiseDecimalPlace = noiseDecimalPlace;
  }

  public boolean isSingletonsForUnconvergedComponents() {
    return singletonsForUnconvergedComponents;
  }

  public void setSingletonsForUnconvergedComponents(boolean singletonsForUnconvergedComponents) {
    this.singletonsForUnconvergedComponents = singletonsForUnconvergedComponents;
  }

  public boolean isAllSourcesClean() {
    return allSourcesClean;
  }

  public void setAllSourcesClean(boolean allSourcesClean) {
    this.allSourcesClean = allSourcesClean;
  }

  public ApType getApType() {
    return apType;
  }

  public void setApType(ApType apType) {
    this.apType = apType;
  }

  public String getSourceDirtinessVertexProperty() {
    return sourceDirtinessVertexProperty;
  }

  public void setSourceDirtinessVertexProperty(String sourceDirtinessVertexProperty) {
    this.sourceDirtinessVertexProperty = sourceDirtinessVertexProperty;
  }

  public List<String> getCleanSources() {
    return cleanSources;
  }

  public void setCleanSources(List<String> cleanSources) {
    this.cleanSources = cleanSources;
  }

  public ApExemplarAssignmentType getApExemplarAssignmentType() {
    return apExemplarAssignmentType;
  }

  public void setApExemplarAssignmentType(ApExemplarAssignmentType apExemplarAssignmentType) {
    this.apExemplarAssignmentType = apExemplarAssignmentType;
  }
}
