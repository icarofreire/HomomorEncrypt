/*
 * 
 */
package deepsea.utilities;

/*
 * Classe para medir tempo de execução de código;
 */

/*
  Ex:
  TimeExecution.inicio(); // << inicio do código a ser médido;

  // ... código...;

  TimeExecution.fim(); // << fim do código a ser médido;
  TimeExecution.exibirTempo(); // << exibir tempo de duração de execução do código acima;
*/
public class TimeExecution {

    private static long startTime_milli;
    private static long endTime_milli;
    private static long startTime_nano;
    private static long endTime_nano;

    public static void inicio() {
      startTime_milli = System.currentTimeMillis();
      startTime_nano = System.nanoTime();
    }

    public static void fim() {
      endTime_milli = System.currentTimeMillis();
      endTime_nano = System.nanoTime();
    }

    public static void exibirTempo() {
        long duration_milli = (endTime_milli - startTime_milli);
        long duration_nano = (endTime_nano - startTime_nano);
        System.out.println( "###########################" );
        System.out.println( "### Time Execution: ###" );
        System.out.println( (duration_milli / 1000.0) + " : seconds." );
        System.out.println( duration_milli + " : milliseconds." );
        System.out.println( duration_nano + " : nanoseconds." );
        System.out.println( "###########################" );
    }

}
