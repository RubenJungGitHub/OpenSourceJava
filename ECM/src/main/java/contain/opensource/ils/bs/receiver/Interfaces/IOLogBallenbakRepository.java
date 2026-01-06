package contain.opensource.ils.bs.receiver.Interfaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;

@Repository
public interface IOLogBallenbakRepository extends JpaRepository<IOLogBallenbak, String> {
    // You can add custom queries here if needed
}
