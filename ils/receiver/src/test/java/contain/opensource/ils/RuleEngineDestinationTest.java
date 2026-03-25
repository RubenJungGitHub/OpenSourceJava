package contain.opensource.ils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.services.ruleengineservice;
import contain.opensource.shared.constants.AlfrescoConstants;
//import contain.opensource.shared.constants.AlfrescoConstants.ContainPlatforms;

@SpringBootTest
class RuleEngineDestinationTest {

    @Autowired
    ruleengineservice reservice;

    @Test
    void CheckDestinationsFromRuleEngine() {
        int failCount = 0; // 1. Track the failures
        AlfrescoConstants.ContainPlatforms containplatformfrom[] = { AlfrescoConstants.ContainPlatforms.SPO };
        String[] marking = { "HR Confidential", "Commercial", "Internal discussion", "Medical Confidentieel",
                "Releasable to: organization", "Yes only:" };
        String[] classification = { "Secret" };

        String expectedresult = "";
        String Message = "";
        for (AlfrescoConstants.ContainPlatforms platform : containplatformfrom) {
            for (String classif : classification) {
                for (String mark : marking) {
                    RelocateInformationObject request = new RelocateInformationObject();
                    expectedresult = classif + "TEST" + platform + mark;
                    request.platformfrom = platform;
                    request.setcontainerfrom("TEST");
                    request.classification = classif;
                    request.marking = mark;
                    try {
                        Message = "Looking for expectedresult -> " + expectedresult;
                        System.out.println(Message);

                        reservice.executeDMN(request);
                        if (!request.getcontainerto().equals(expectedresult)
                                || !request.getplatformto().equals(AlfrescoConstants.ContainPlatforms.TEST)) {
                            Message = String.format(
                                    "FAILED: Input[%s, %s] | Expected platformto : [%s] | Got: [%s] | Expectedcontainerto : [%s] | Got: [%s]",
                                    classif, mark, expectedresult, request.getcontainerfrom(), "TEST",
                                    request.getcontainerfrom());
                            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                                    + Message
                                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                            failCount++; // 1. Track the failures

                        } else {
                            Message = String.format(
                                    "SUCCESS: Input[%s, %s] | Expected platformto : [%s] | Got: [%s] | Expectedcontainerto : [%s] | Got: [%s]",
                                    classif, mark, expectedresult, request.getcontainerfrom(), "TEST",
                                    request.getcontainerfrom());

                            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                                    + Message
                                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                        }
                    } catch (Exception e) {
                        Message = String
                                .format("FAILED: NULL RETURNED FROM RuleEngine : Expectedresult ->" + expectedresult);
                        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                                + Message
                                + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                        failCount++; // 1. Track the failures
                    }
                }
            }
        }
        // 3. This is the magic line that makes the test fail "officially"
        if (failCount > 0) {
            throw new RuntimeException("STOP! " + failCount + " mismatches found in DMN logic.");
        }
        System.out.println("Test completed -> Rule Engine : " + failCount + " mismatches found.");
    }
}
