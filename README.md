# DeepSea
Software para buscas e transferências de arquivos DICOM entre servidores.

## Development
Executar projeto:
```
$ gradle run
```

Compilar projeto:
```
$ gradle build
```

Local do arquivo .jar gerado:
```
$ DeepSea/app/build/libs/
```

## Arquivo de configuração
Um arquivo de configuração vederá ser criado para descrever as credênciais
de conexão SSH dos servidores que o DeepSea estabelecerá conexão.
O arquivo será criado executando a  opção `-c` ao executar o `deepsea.jar`:
```
$ java -jar deepsea.jar -c
```
Escreva as credênciais por conexão SSH dos servidores no arquivo e execute o programa.

## Logs
Em casos de erros de conexão com bancos ou servidores, será criado um arquivo de log para
expor este problema.
`error.log`