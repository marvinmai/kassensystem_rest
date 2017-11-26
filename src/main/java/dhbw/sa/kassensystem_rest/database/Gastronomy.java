package dhbw.sa.kassensystem_rest.database;

/**
 * Stellt Informationen über die Gastronomie bereit. Die Klasse soll mit einem Eintrag in der
 * MySQL-Datenbank ersetzt werden.
 *
 * @author Marvin Mai
 */
public class Gastronomy {

    public static String getName() {
        /*
      TODO ergänzen eines Datensatzes in der Datenbank, damit aus dem GUI die Gastro-Anschrift bearbeitet werden kann
     */
        return "Restaurante Gaumenfreude";
    }

    public static String getAdress() {
        return "Gourmetstraße 11\n12345 Leckerschmeckerhausen";
    }

    public static String getTelephonenumber() {
        return "+49 541 466 655";
    }
}