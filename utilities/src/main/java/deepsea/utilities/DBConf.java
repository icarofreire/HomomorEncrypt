/*
 * 
 */
package deepsea.utilities;

/**
 * Classe com credenciais de bancos utilizados pelo projeto;
 */
public final class DBConf {

    /*\/ credenciais banco principal -- 
    onde serão armazenados os arquivos mais recentes; */
    public static final String ipPortaPri = "172.25.190.10:5432";
    public static final String bancoPri = "compact_dicoms";
    public static final String usuarioPri = "postgres";
    public static final String senhaPri = "PpSes2020!2019ProdPass";

    /*\/ credenciais banco secundário -- 
    onde serão transferidos os arquivos antigos,
    antecessores a N meses; */
    public static final String ipPortaSec = "172.25.190.10:5432";
    public static final String bancoSec = "compact_old_dicoms";
    public static final String usuarioSec = "postgres";
    public static final String senhaSec = "PpSes2020!2019ProdPass";
}