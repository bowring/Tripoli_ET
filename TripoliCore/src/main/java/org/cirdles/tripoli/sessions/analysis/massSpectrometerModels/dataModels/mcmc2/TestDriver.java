/*
 * Copyright 2022 James Bowring, Noah McLean, Scott Burdick, and CIRDLES.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.dataModels.mcmc2;

import com.google.common.primitives.Doubles;
import jama.Matrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.cirdles.commons.util.ResourceExtractor;
import org.cirdles.tripoli.Tripoli;
import org.cirdles.tripoli.plots.PlotBuilder;
import org.cirdles.tripoli.plots.histograms.HistogramBuilder;
import org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.detectorSetups.Detector;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.StrictMath.log;
import static org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.dataModels.mcmc2.DataUtilities.*;
import static org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.dataModels.mcmc2.MathUtilities.extractColumn;

public enum TestDriver {
    ;

    public static PlotBuilder[] simulationsOfMCMC2() {
        // TODO: https://stackoverflow.com/questions/1881172/matlab-matrix-functions-in-java

        double[] truthModel = {log(0.3), log(2e6), -1e2, 2e2};

        MCMC2SetupRecord setup = new MCMC2SetupRecord(
                (int) 1.0e2, (int) 1.0e2,
                new Detector(Detector.DetectorTypeEnum.FARADAY, "F1", 0, Detector.AmplifierTypeEnum.RESISTANCE, 1.0e11, 1, 0, 0),
                new double[(int) 1.0e2],
                new double[(int) 1.0e2], null, (int) 2.0e4, 20, 4/*todo: use truth count*/, 8, 10, 10, 50, 0)
                .initializeIntegrationTimes(1, 1);

        MCMC2ResultsRecord[] allResults = new MCMC2ResultsRecord[setup.simulationsCount()];

        double[][] allModels = new double[0][];

        for (int simulationIndex = 0; simulationIndex < setup.simulationsCount(); simulationIndex++) {
            // todo: research thread safe time handlers
//            long startTime = System.nanoTime();
            long startTime = System.currentTimeMillis();
            MCMC2DataRecord mcmc2DataRecord = syntheticData(simulationIndex + 1);

            MaxLikelihoodRecord maxLikelihoodRecord = maxLikelihood(mcmc2DataRecord, setup);
            setup = setup.updateRecordWithCovariance(maxLikelihoodRecord.covarianceMatrix());

            MCMC2ChainRecord mcmc2ChainRecord = initializeChains(setup, mcmc2DataRecord, maxLikelihoodRecord);
            double[][] initModels = mcmc2ChainRecord.initModels();
            double[] initLogLiks = mcmc2ChainRecord.initLogLiks();

            int nSavedModels = setup.MCMCTrialsCount() / setup.seive();
            double[][][] modelChains = new double[setup.modelParameterCount()][nSavedModels][setup.chainsCount()];
            for (int i = 0; i < setup.modelParameterCount(); i++) {
                for (int j = 0; j < nSavedModels; j++) {
                    Arrays.fill(modelChains[i][j], Double.NaN);
                }
            }
            double[][] loglikChains = new double[nSavedModels][setup.chainsCount()];
            for (int j = 0; j < nSavedModels; j++) {
                Arrays.fill(loglikChains[j], Double.NaN);
            }

//        parfor iChain = 1:setup.nChains
            double[][] outputModels = null;
            double[] outputLogLiks = null;

            for (int iChain = 0; iChain < setup.chainsCount(); iChain++) {
                MetropolisHastingsRecord metropolisHastingsRecord = metropolisHastings(
                        iChain,
                        extractColumn(initModels, iChain),
                        initLogLiks[iChain],
                        mcmc2DataRecord,
                        setup);

                outputModels = metropolisHastingsRecord.outputModels();
                outputLogLiks = metropolisHastingsRecord.outputLogLiks();

                for (int i = 0; i < setup.modelParameterCount(); i++) {
                    for (int j = 0; j < nSavedModels; j++) {
                        modelChains[i][j][iChain] = outputModels[i][j];
                        loglikChains[j][iChain] = outputLogLiks[j];
                    }
                }
            } // iChain

            double[][][] postBurnInChains = new double[setup.modelParameterCount()][modelChains[0].length - setup.burnIn()][setup.chainsCount()];
            for (int i = 0; i < setup.modelParameterCount(); i++) {
                for (int j = 0; j < nSavedModels - setup.burnIn(); j++) {
                    for (int k = 0; k < setup.chainsCount(); k++) {
                        postBurnInChains[i][j][k] = modelChains[i][j + setup.burnIn()][k];
                    }
                }
            }

            /*
                % aggregate chains
                    setup.nPostBurnIn = size(postBurnInChains, 2);
                    mAll = reshape(postBurnInChains, [setup.nmodel, setup.nPostBurnIn*setup.nChains]);
                    result(iSim).modelMean = mean(mAll,2);
                    result(iSim).modelCov = cov(mAll');

                    % calculate chiSqare with true values
                    result(iSim).r = result(iSim).modelMean - truth.model;
                    result(iSim).ChiSq = result(iSim).r' * inv(result(iSim).modelCov) * result(iSim).r;
             */

            setup = setup.updateRecordWithPostBurnIn(nSavedModels - setup.burnIn());
            allModels = new double[setup.modelParameterCount()][setup.postBurnInCount() * setup.chainsCount()];
            double[] sums = new double[setup.modelParameterCount()];
            for (int i = 0; i < setup.modelParameterCount(); i++) {
                for (int j = 0; j < setup.postBurnInCount(); j++) {
                    for (int k = 0; k < setup.chainsCount(); k++) {
                        allModels[i][j + k * setup.postBurnInCount()] = postBurnInChains[i][j][k];
                        sums[i] += postBurnInChains[i][j][k];
                    }
                }
            }
            double[] modelMeans = new double[setup.modelParameterCount()];
            double[] rArray = new double[setup.modelParameterCount()];
            for (int i = 0; i < setup.modelParameterCount(); i++) {
                modelMeans[i] = sums[i] / (setup.postBurnInCount() * setup.chainsCount());
                rArray[i] = modelMeans[i] - truthModel[i];
            }

            // todo: review this
            Matrix allModelsTranspose = new Matrix(allModels).transpose();
            double[][] modelCov = (new Covariance(allModelsTranspose.getArray())).getCovarianceMatrix().getData();

            Matrix modelCovMatrix = new Matrix(modelCov);
            Matrix rMatrix = new Matrix(rArray, rArray.length);
            double chiSquare = rMatrix.transpose().times(modelCovMatrix.inverse()).times(rMatrix).get(0, 0);

            allResults[simulationIndex] = new MCMC2ResultsRecord(simulationIndex, modelMeans, modelCov, rArray, chiSquare);

            if ((1 + simulationIndex) % 10 == 0) {
                System.out.println("simulation " + simulationIndex + "   millisecs: " + (System.currentTimeMillis() - startTime));//              ((System.nanoTime() - startTime) / 1000000000));
            }
        } // simulationIndex

        PlotBuilder histogramBuilderA = HistogramBuilder.initializeHistogram(1, allModels[0],
                100, new String[]{"IsoA"}, "Counts", "Frequency", true);
        PlotBuilder histogramBuilderB = HistogramBuilder.initializeHistogram(1, allModels[1],
                100, new String[]{"IsoB"}, "Counts", "Frequency", true);
        PlotBuilder histogramBuilderC = HistogramBuilder.initializeHistogram(1, allModels[2],
                100, new String[]{"BLA"}, "Counts", "Frequency", true);
        PlotBuilder histogramBuilderD = HistogramBuilder.initializeHistogram(1, allModels[3],
                100, new String[]{"BLB"}, "Counts", "Frequency", true);


        return new PlotBuilder[]{histogramBuilderA, histogramBuilderB, histogramBuilderC, histogramBuilderD};

//        System.out.println("END OF DEMO ");
    }


    static boolean[] logicalAnd(boolean[] arrayA, boolean[] arrayB) {
        boolean[] logicalAnd = new boolean[arrayA.length];
        for (int i = 0; i < arrayA.length; i++) {
            logicalAnd[i] = arrayA[i] & arrayB[i];
        }
        return logicalAnd;
    }

    static double[] extractDoubleData(int iSim, String fileName) {
        List<String> contentsByLine = extractFileContentsByLine(iSim, fileName);
        String[] contentsByLineArray = contentsByLine.toArray(new String[0]);
        double[] contentsAsDoubles = Arrays.stream(contentsByLineArray)
                .mapToDouble(Double::parseDouble)
                .toArray();
        return contentsAsDoubles;
    }

    static boolean[] extractBooleanData(int iSim, String fileName) {
        List<String> contentsByLine = extractFileContentsByLine(iSim, fileName);
        boolean[] contentsAsBooleans = new boolean[contentsByLine.size()];
        String[] contentsByLineArray = contentsByLine.toArray(new String[0]);
        for (int i = 0; i < contentsAsBooleans.length; i++) {
            contentsAsBooleans[i] = 1 == Integer.parseInt(contentsByLineArray[i]);
        }
        return contentsAsBooleans;
    }

    private static List<String> extractFileContentsByLine(int iSim, String fileName) {
        ResourceExtractor RESOURCE_EXTRACTOR
                = new ResourceExtractor(Tripoli.class);
        List<String> contentsByLine = new ArrayList<>();
        try {
            Path dataPath = RESOURCE_EXTRACTOR
                    .extractResourceAsFile("/org/cirdles/tripoli/dataSourceProcessors/dataSources/syntheticData/SyntheticOutToTripoli/"
                            + iSim + "_Sim/" + fileName).toPath();
            contentsByLine.addAll(Files.readAllLines(dataPath, Charset.defaultCharset()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentsByLine;
    }

    static boolean[] filterDataByValue(double[] source, double value) {
        boolean[] extracted = new boolean[source.length];
        for (int i = 0; i < source.length; i++) {
            extracted[i] = (source[i] == value);
        }
        return extracted;
    }

    static double[] filterDataByFlags(double[] source, boolean[] selector) {
        List<Double> retrievedValuesList = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            if (selector[i]) {
                retrievedValuesList.add(source[i]);
            }
        }
        return Doubles.toArray(retrievedValuesList);
    }


    static boolean[] invertSelector(boolean[] selector) {
        boolean[] invertedSelector = new boolean[selector.length];
        for (int i = 0; i < selector.length; i++) {
            invertedSelector[i] = !selector[i];
        }
        return invertedSelector;
    }


}
