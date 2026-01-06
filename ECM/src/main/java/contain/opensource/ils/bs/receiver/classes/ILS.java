package contain.opensource.ils.bs.receiver.classes;
import static org.fusesource.jansi.Ansi.ansi;
import org.springframework.stereotype.Component;

@Component
public class ILS {

        public static void SayHello(String[] args) {
                if (args == null || args.length == 0) {
                        args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
                }
                String from = "DefaultFrom";
                String to = "DefaultTo";

                for (int i = 0; i < args.length; i++) {
                        switch (args[i]) {
                                case "--from":
                                        if (i + 1 < args.length)
                                                from = args[i + 1];
                                        i++; // skip value
                                        break;
                                case "--to":
                                        if (i + 1 < args.length)
                                                to = args[i + 1];
                                        i++;
                                        break;
                        }
                }

                System.out.println(ansi().fgRed().a("Hello world, ")
                                .fgBlue().a(to)
                                .fgGreen().a(" from " + from + "!")
                                .reset());

                // System.out.println("This text should appear in the default color.");
        }
}