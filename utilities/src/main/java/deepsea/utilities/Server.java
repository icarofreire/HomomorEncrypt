package deepsea.utilities;

public final class Server {

    private String host;
    private String username;
    private String password;
    /*\/ pasta base no servidor onde as buscas serão realizadas; */
    private String folderBase;

    public Server(String host, String username, String password, String folderBase) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.folderBase = folderBase;
    }

    /**
	* Returns value of host
	* @return
	*/
	public String getHost() {
		return host;
	}

	/**
	* Sets new value of host
	* @param
	*/
	public void setHost(String host) {
		this.host = host;
	}

    /**
	* Returns value of username
	* @return
	*/
	public String getUsername() {
		return username;
	}

	/**
	* Sets new value of username
	* @param
	*/
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	* Returns value of password
	* @return
	*/
	public String getPassword() {
		return password;
	}

	/**
	* Sets new value of password
	* @param
	*/
	public void setPassword(String password) {
		this.password = password;
	}

    /**
	* Returns value of folderBase
	* @return
	*/
	public String getFolderBase() {
		return folderBase;
	}

	/**
	* Sets new value of folderBase
	* @param
	*/
	public void setFolderBase(String folderBase) {
		this.folderBase = folderBase;
	}
    
}