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

package org.cirdles.tripoli.sessions.analysis.massSpectrometerModels;

import org.cirdles.commons.util.ResourceExtractor;
import org.cirdles.tripoli.Tripoli;
import org.cirdles.tripoli.sessions.Session;
import org.cirdles.tripoli.sessions.analysis.Analysis;
import org.cirdles.tripoli.sessions.analysis.methods.AnalysisMethodBuiltinFactory;
import org.cirdles.tripoli.sessions.analysis.samples.Sample;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.cirdles.tripoli.sessions.analysis.methods.AnalysisMethodBuiltinFactory.BURDICK_BL_SYNTHETIC_DATA;
import static org.cirdles.tripoli.sessions.analysis.methods.AnalysisMethodBuiltinFactory.KU_204_5_6_7_8_DALY_ALL_FARADAY_PB;

/**
 * @author James F. Bowring
 */
public final class SessionBuiltinFactory {

    public static final Map<String, Session> sessionsBuiltinMap = new TreeMap<>();
    public static String TRIPOLI_DEMONSTRATION_SESSION = "Tripoli Demonstration Session";

    private static ResourceExtractor RESOURCE_EXTRACTOR = new ResourceExtractor(Tripoli.class);
    static {
        Session tripoliDemonstrationSession = Session.initializeSession(TRIPOLI_DEMONSTRATION_SESSION);
        tripoliDemonstrationSession.setMutable(false);
        sessionsBuiltinMap.put(tripoliDemonstrationSession.getSessionName(), tripoliDemonstrationSession);

        Sample twoIsotopeSample_01 = new Sample();
        Analysis twoIsotopes_01 = Analysis.initializeAnalysis("Two Isotope Demo_01", AnalysisMethodBuiltinFactory.analysisMethodsBuiltinMap.get(BURDICK_BL_SYNTHETIC_DATA), twoIsotopeSample_01);
        Path dataFilePath = RESOURCE_EXTRACTOR
                .extractResourceAsFile("/org/cirdles/tripoli/dataProcessors/dataSources/synthetic/twoIsotopeSyntheticData/SyntheticDataset_01.txt").toPath();
        twoIsotopes_01.setDataFilePath(dataFilePath);
        tripoliDemonstrationSession.addAnalysis(twoIsotopes_01);

        Sample fiveIsotopeSample_01 = new Sample();
        Analysis fiveIsotopes_01 = Analysis.initializeAnalysis("Five Isotope Demo_01", AnalysisMethodBuiltinFactory.analysisMethodsBuiltinMap.get(KU_204_5_6_7_8_DALY_ALL_FARADAY_PB), fiveIsotopeSample_01);
        dataFilePath = RESOURCE_EXTRACTOR
                .extractResourceAsFile("/org/cirdles/tripoli/dataProcessors/dataSources/synthetic/fiveIsotopeSyntheticData/SyntheticDataset_01R.txt").toPath();
        fiveIsotopes_01.setDataFilePath(dataFilePath);
        tripoliDemonstrationSession.addAnalysis(fiveIsotopes_01);

    }
}