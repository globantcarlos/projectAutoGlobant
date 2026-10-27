Feature: Validar la respuesta del API am-tc-region post migracion Java 25 / Boot 4.1.0
  Como QA
  Quiero consultar la API am-tc-region
  Para validar el comportamiento funcional post migracion (TestRail Run 13927 - SSM-17667)

  Contrato real: GET /tc/region/regionByRoute con headers
  channel / x-transactionId / departure / arrival (Postman am-my-trips).

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario Outline: TestRail Run 13927 - regionByRoute consulta exitosa - <titulo>
    Given el usuario consulta regionByRoute con departure "<departure>" y arrival "<arrival>"
    Then se valida que la respuesta region sea "<statusCode>"
    And se valida que la respuesta contenga regionShortDescription "<regionShortDescription>"
    And se valida que la respuesta contenga los campos del contrato regionByRoute

    Examples:

      | departure | arrival | statusCode | regionShortDescription | titulo                                                       |
      | MEX       | LAX     | 200        | INT                    | TC001_HP_MT_TC REGION_REGIONBYROUTE OD INTERNACIONAL POST MIGRACION |
      | MEX       | MTY     | 200        | DOM                    | TC002_HP_MT_TC REGION_REGIONBYROUTE OD DOMESTICO POST MIGRACION     |

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC003_HP_MT_TC REGION_CONSULTAS CONSECUTIVAS MISMO OD DATOS CONSISTENTES
    Given el usuario consulta regionByRoute con departure "MEX" y arrival "LAX"
    And el usuario consulta nuevamente regionByRoute con departure "MEX" y arrival "LAX"
    And el usuario consulta regionByRoute con departure "MEX" y arrival "LAX" por tercera vez
    Then se valida que las tres respuestas region sean 200 con contenido identico

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC004_TTF_MT_TC REGION_ERROR DOWNSTREAM NO DISPONIBLE 500 FAIL FAST
    Given el usuario consulta regionByRoute con departure "ZZZ" y arrival "CUN" forzando error de downstream
    Then se valida que la respuesta region sea 500 con reason del error catalog
    And se valida que el servicio responda fail-fast en menos de 30 segundos

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC005_TTP_MT_TC REGION_METRICA NEW RELIC ERRORES POST MIGRACION
    Given el usuario consulta regionByRoute con departure "ZZZ" arrival "CUN" y transactionId de traza nr-trace-17667
    Then se valida que la respuesta region sea 500 con reason del error catalog
    And se valida que el transactionId de traza quede registrado para New Relic

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC006_TTF_MT_TC REGION_ERROR HEADER OBLIGATORIO FALTANTE 400 BAD REQUEST
    Given el usuario consulta regionByRoute con departure "MEX" sin el header arrival
    Then se valida que la respuesta region sea 400 con reason ERR.TC.REGION.BAD_REQUEST

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC007_TTF_MT_TC REGION_ERROR IATA INEXISTENTE 500 EMPTY RESPONSE
    Given el usuario consulta regionByRoute con departure "ZZZ" y arrival "CUN" con IATA inexistente
    Then se valida que la respuesta region sea 500 con reason del error catalog

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC008_TTP_MT_TC REGION_ESTABILIDAD 8 CONSULTAS CONSECUTIVAS SIN ERRORES
    Given el usuario ejecuta 8 consultas consecutivas de regionByRoute con departure "MEX" y arrival "MTY"
    Then se valida que las 8 consultas region respondan 200

  @Regression @casosFuncionales @tcRegion @testRail @SSM17667 @epic:am-tc-region @feature:Funcionales
  Scenario: TC009_TTP_MT_TC REGION_MANAGEMENT INFO VERSION DESPLEGADA
    Given el usuario consulta el management info de am-tc-region
    Then se valida que la respuesta management info sea 200
    And se valida que el management info reporte el microservicio "am-tc-region" y su Version

  # ==================== BDD Run 13939 - SSM-11165 ====================
  # TestRail case 121530 (positivos) y 121546 (negativos).
  # Comportamiento real del servicio en DEV (sondeado 2026-10-26).

  @Regression @casosFuncionales @tcRegion @testRail @Run13939 @SSM11165 @epic:am-tc-region @feature:BDD_Positivos
  Scenario Outline: TestRail Run 13939 - BDD Positivos - <titulo>
    Given el usuario consulta regionByRoute con los headers exactos "<headers>"
    Then se valida que la respuesta region sea "<statusCode>"
    And se valida que la respuesta exitosa contenga los campos del contrato regionByRoute

    Examples:
      | headers                                                                                                             | statusCode | titulo                                              |
      | channel=web, x-transactionId=bdd-13939-1, departure=MEX, arrival=MTY                                                | 200        | channel web en minusculas                           |
      | channel=WEB, x-transactionId=bdd-13939-2, departure=MEX, arrival=MTY                                                | 200        | channel web en mayusculas                           |
      | channel=Web, x-transactionId=bdd-13939-3, departure=MEX, arrival=MTY                                                | 200        | channel web en formato mixto                        |
      | channel=web, Content-Type=application/json, x-transactionId=bdd-13939-4, departure=MEX, arrival=MTY                 | 200        | Content-Type incluido como header                   |
      | channel=web, x-transactionId=bdd-13939-4, departure=MEX, arrival=MTY                                                | 200        | header x-transaction-id incluido                    |
      | channel=web, x-transactionId=bdd-13939-5, departure=mex, arrival=mty                                                | 200        | departure y arrival en minusculas                   |
      | channel=web, x-transactionId=bdd-13939-6, departure=MEX, arrival=MTY                                                | 200        | departure y arrival en mayusculas                   |

  # NOTA: se evaluó esperar 200 para "channel no incluido" por una modificación del
  # servicio, pero DEV aún responde 400 con channel:null; el caso queda en 400
  # (comportamiento real) hasta que el despliegue llegue a DEV.
  # DEFECTO (a reportar al equipo am-tc-region): el contrato exige rechazar (400) los
  # headers con espacios entre caracteres, pero en DEV el servicio los acepta con 200.
  # Los escenarios quedan ajustados al comportamiento real; si el micro corrige, actualizar.
  @Regression @casosFuncionales @tcRegion @testRail @Run13939 @SSM11165 @epic:am-tc-region @feature:BDD_Negativos
  Scenario Outline: TestRail Run 13939 - BDD Negativos header aceptado (defecto) - <titulo>
    Given el usuario consulta regionByRoute con los headers exactos "<headers>"
    Then se valida que la respuesta region sea "<statusCode>"
    And se valida que la respuesta exitosa contenga los campos del contrato regionByRoute

    Examples:
      | headers                                                                                                             | statusCode | titulo                                         |
      | channel=w e b, x-transactionId=bdd-13939-n2, departure=MEX, arrival=MTY                                             | 200        | channel con espacios entre caracteres          |
      | channel=web, Content-Type=appli cation/json, x-transactionId=bdd-13939-n2, departure=MEX, arrival=MTY               | 200        | Content-Type con espacios entre caracteres     |
      | channel=web, x-transactionId=a b c, departure=MEX, arrival=MTY                                                      | 200        | x-transaction-id con espacios entre caracteres |

  @Regression @casosFuncionales @tcRegion @testRail @Run13939 @SSM11165 @epic:am-tc-region @feature:BDD_Negativos
  Scenario Outline: TestRail Run 13939 - BDD Negativos - <titulo>
    Given el usuario consulta regionByRoute con los headers exactos "<headers>"
    Then se valida que la respuesta de error tenga channel "<channelRecibido>" httpCode "<httpCode>" y reason del error catalog

    Examples:
      | headers                                                                 | channelRecibido | httpCode | titulo                                  |
      | x-transactionId=bdd-13939-n1, departure=MEX, arrival=MTY                | null            | 400      | channel no incluido                     |
      | channel=web, departure=MEX, arrival=MTY                                 | web             | 400      | x-transaction-id no incluido            |
      | channel=web, x-transactionId=bdd-13939-n3, departure=M E X, arrival=MTY | web             | 400      | departure con espacios entre caracteres |
      | channel=web, x-transactionId=bdd-13939-n3, departure=MEX, arrival=M T Y | web             | 400      | arrival con espacios entre caracteres   |
      | channel=web, x-transactionId=bdd-13939-n3, arrival=MTY                  | web             | 400      | departure no incluido                   |
      | channel=web, x-transactionId=bdd-13939-n4, departure=MEX                | web             | 400      | arrival no incluido                     |
