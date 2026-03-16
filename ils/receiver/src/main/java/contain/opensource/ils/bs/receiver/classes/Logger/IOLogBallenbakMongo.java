package contain.opensource.ils.bs.receiver.classes.Logger;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tbl_iolog")
public class IOLogBallenbakMongo {

    @Id
    private String id;

    @Field("platform_id")
    private String platformId;

    private String path;

    private String marking;

    @Field("classification")
    private String classification;

    private String version;

    @Field("contain_io_uuid")
    private String containIoUuid;

    @Field("io_action")
    private String ioAction;

    @Field("io_source")
    private String ioSource;

    @Field("io_destination")
    private String ioDestination;

    @Field("pki_hash")
    private String pkiHash;

    @Field("io_reference")
    private String ioReference;

    @Field("additional_info")
    private String additionalInfo;

    @Field("log_datetime")
    private LocalDateTime logDateTime;

    @Field("action_performed")
    private eActionPerformed actionPerformed;

    @Field("action_performed_by")
    private String actionPerformedBy;
}