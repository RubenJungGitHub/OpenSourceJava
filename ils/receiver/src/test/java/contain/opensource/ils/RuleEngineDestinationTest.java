package contain.opensource.ils;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.constants.AlfrescoConstants;

@SpringBootTest
class RuleEngineDestinationTest {

    private class RelocateInformationDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        // Use plain Strings to match your BC Data Types exactly
        public String containplatformfrom;
        public String classification;
        public String marking;

        // Standard empty constructor
        public RelocateInformationDTO() {
        }

        // Convenience constructor
        public RelocateInformationDTO(String platform, String classification, String marking) {
            this.containplatformfrom = platform;
            this.classification = classification;
            this.marking = marking;
        }
    }

    @Autowired
    migrationservice migservice;

    @Test
    void CheckDestinationsFromRuleEngine() {
        AlfrescoConstants.ContainPlatforms containplatformfrom[] = { AlfrescoConstants.ContainPlatforms.SPO };
        String[] classification = { "HR Confidential", "Unclassified", "Intenral", "Conficential", "Secret" };
        String[] marking = { "Public", "Unclassified", "Medical Confidentieel", "Internal discussion", "Yes only:",
                "Releasable to: organization" };
        RelocateInformationObject ROobject = new RelocateInformationObject();
        String expectedresult = "";
        String Message = "";
        for (AlfrescoConstants.ContainPlatforms platform : containplatformfrom) {
            for (String classif : classification) {
                for (String mark : marking) {
                    expectedresult = classif + platform + platform + mark;
                    ROobject.containplatformfrom = platform;
                    ROobject.classification = classif;
                    ROobject.marking = mark;
                    Object actualResult = migservice.executeDMN(ROobject);
                    if (!actualResult.equals(expectedresult)) {
                        Message = String.format("FAILED: Input[%s, %s] | Expected: [%s] | Got: [%s]",
                                classif, mark, expectedresult, actualResult);
                        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                                + Message
                                + contain.opensource.shared.constants.AlfrescoConstants.RESET);

                    } else {
                        Message = String.format("SUCCESS: Input[%s, %s] | Expected: [%s] | Got: [%s]",
                                classif, mark, expectedresult, actualResult);

                        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                                + Message
                                + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                    }
                }
            }
        }
        System.out.println("Test completed");
    }
}
