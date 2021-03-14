package org.matsim.project;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

public class RunFAIT {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public void runFreightAnalysis(){
		try{
			RunChessboardWithFreight rc = new RunChessboardWithFreight();
			Config config = rc.prepareConfig();
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setLastIteration(0);
			rc.run();
		} catch (Exception e){
			e.printStackTrace();
			Assert.fail("Something about Chessboard w/ matsim-freight doesn't work.");
		}
	}
}
