# Analizador Final-
Lenguajes y Automatas II Grupo: 6N1 
Docente:Echeverria Rodrigez Cesar Osvaldo
# Integrantes del Equipo
 Fabiola Bernal Sanchez
 Emmanuel Cardoza Esquivel
 Omar Orozco 
 # Numero de Equipo 
 2
 # Analizador Léxico, Sintáctico y Semántico
 # Descripción
Este proyecto es un programa desarrollado en Java que simula las primeras tres fases de un compilador análisis léxico, análisis sintáctico y análisis semántico aplicadas a un subconjunto simplificado del lenguaje ensamblador del procesador Intel 8086. El programa recibe como entrada un bloque de texto con instrucciones aritméticas (ADD, SUB, INC, DEC) que operan sobre registros de 8 bits, además de declaraciones de variables con tipo (NUM para valores numéricos y CHAR para caracteres), y determina si ese texto es correcto en cada uno de los tres niveles de análisis, mostrando el resultado tanto en consola como en una interfaz gráfica.

## Objetivo
El objetivo principal es aplicar de forma práctica los conceptos de teoría de autómatas y compiladores, implementando cada fase del análisis como una máquina de estados finita programada manualmente, sin apoyarse en expresiones regulares ni en funciones de librería para clasificar caracteres (como `Character.isLetter`). Con esto se busca:
- Reconocer y validar los componentes léxicos (tokens) de las instrucciones del 8086.
- Verificar que las instrucciones estén correctamente estructuradas (cantidad y orden de operandos).
- Detectar errores de significado que no son visibles a simple vista, como combinar operandos de tipos de datos incompatibles.
- Presentar los resultados del análisis de forma clara y visual, facilitando la comprensión del proceso.

## Tecnologías utilizadas
- **Lenguaje de programación:** Java .
- **Interfaz gráfica:** Java Swing (`JFrame`, `JTable`, `JTextArea`, `JOptionPane`.
- **Sin librerías externas:** todo el análisis se implementa con clases estándar de Java (`ArrayList`, `HashMap`/`LinkedHashMap`), sin frameworks de terceros.
- **Sin expresiones regulares ni funciones ctype predefinidas:** la clasificación de cada carácter (letra, dígito, espacio, coma, salto de línea, símbolo inválido) se hace comparando manualmente contra rangos ASCII, cumpliendo con la restricción de no usar herramientas de reconocimiento de patrones ya construidas.

## Instrucciones reconocidas
El lenguaje soporta cuatro instrucciones aritméticas, cada una con una cantidad fija de operandos:

| Instrucción | Operandos | Descripción |
|---|---|---|
| `ADD destino, origen` | 2 | Suma el valor de origen al destino |
| `SUB destino, origen` | 2 | Resta el valor de origen al destino |
| `INC operando` | 1 | Incrementa en 1 el operando |
| `DEC operando` | 1 | Decrementa en 1 el operando |

Además, se reconocen declaraciones de variables con la palabra clave del tipo de dato seguida del nombre de la variable:
- `NUM nombre` → declara una variable numérica.
- `CHAR nombre` → declara una variable de tipo carácter.

## Registros conocidos
El programa reconoce los ocho registros de propósito general de 8 bits del procesador 8086 (mitades alta y baja de los registros de 16 bits AX, BX, CX y DX):

`AL`, `AH`, `BL`, `BH`, `CL`, `CH`, `DL`, `DH`

Todos los registros se consideran, para efectos del análisis semántico, de tipo **NUMÉRICO**, ya que almacenan valores de 8 bits.

## Analizador léxico
Es la primera fase del programa. Recorre la cadena de entrada **carácter por carácter** mediante un autómata de estados finitos implementado con estructuras `switch-case`, sin usar expresiones regulares. Los estados principales son: *inicio*, *leyendo palabra*, *leyendo número* y *error*. Cada carácter se clasifica manualmente en una de estas categorías: letra, dígito, espacio/tabulador, coma, salto de línea, retorno de carro o símbolo inválido.

Como resultado, el analizador léxico produce una lista de **tokens**, cada uno con su lexema, tipo y posición (línea y columna). Los tipos de token reconocidos son:
- **OPERADOR** (ADD, SUB, INC, DEC)
- **REGISTRO** (AL, AH, BL, BH, CL, CH, DL, DH)
- **DATO** (palabras clave de tipo: NUM, CHAR)
- **IDENTIFICADOR** (nombres de variables definidos por el usuario)
- **LITERAL_NUMÉRICO** (secuencias de dígitos)
- **COMA** y **ESPACIO** (separadores)
- **DESCONOCIDO** (cualquier símbolo que no pertenezca al lenguaje, como `$`, `%`, `#`, etc.)

## Analizador sintáctico
Una vez obtenidos los tokens, el analizador sintáctico agrupa los tokens línea por línea y valida que cada línea tenga una **estructura correcta**, sin importar todavía su significado. Por ejemplo, verifica que:
- Una instrucción `ADD`/`SUB` esté seguida de exactamente dos operandos separados por una coma.
- Una instrucción `INC`/`DEC` esté seguida de exactamente un operando.
- Una declaración tenga la forma `TIPO nombre` (una palabra clave de tipo seguida de un identificador).
- No falten ni sobren tokens en la línea (por ejemplo, `ADD AL` sin el segundo operando, o `ADD AL, BH, CL` con un operando de más).

Si la estructura no coincide con ninguno de estos patrones, se reporta un error estructural, independientemente de si las palabras usadas eran válidas o no a nivel léxico.

## Analizador semántico
Es la fase que verifica si las instrucciones, además de estar bien escritas, **tienen sentido**. Para esto, el programa construye una **tabla de símbolos**: un registro que asocia cada nombre de variable declarada con su tipo de dato (NUMÉRICO o CARÁCTER), llenado a partir de las declaraciones (`NUM`/`CHAR`) encontradas en el código.

Luego, para cada instrucción, el analizador resuelve el tipo de cada operando (los registros siempre son NUMÉRICO; las variables se buscan en la tabla de símbolos) y valida:
- Que `ADD`/`SUB` combinen **dos operandos del mismo tipo** (no se puede sumar un NUMÉRICO con un CARÁCTER).
- Que `INC`/`DEC` solo se apliquen sobre operandos NUMÉRICOS.
- Que toda variable usada haya sido previamente declarada.
- Que no se declare la misma variable dos veces.

También genera advertencias (no errores fatales) para casos como usar el mismo operando dos veces en una operación.

## Funcionamiento
El flujo completo del programa es el siguiente:
1. El usuario escribe o carga un bloque de instrucciones en el editor de texto de la interfaz.
2. **Fase léxica:** el texto se recorre carácter por carácter y se convierte en una lista de tokens.
3. **Fase sintáctica:** los tokens se agrupan por línea y se valida que cada instrucción tenga la forma correcta.
4. **Fase semántica:** se construye la tabla de símbolos y se valida la compatibilidad de tipos en cada instrucción.
5. Los resultados se muestran en varias vistas: una tabla con todos los tokens reconocidos, un resumen con contadores por categoría, una ventana emergente con el detalle de los errores (léxicos y semánticos) junto con sugerencias de corrección, y una tabla de tipos por instrucción acompañada de un árbol de expresión gráfico que representa visualmente el operador y sus operandos con su tipo resuelto.

## Ejemplo
Bloque de entrada:
```
NUM contador
CHAR letra
ADD AL, BH
ADD contador, letra
```
Resultado del análisis:
- Léxico: todos los tokens son reconocidos correctamente (0 errores léxicos).
- Sintáctico: las cuatro líneas tienen una estructura válida (declaración correcta, instrucciones con la cantidad correcta de operandos).
- Semántico:
  - `ADD AL, BH` → válida (ambos registros son NUMÉRICO).
  - `ADD contador, letra` → **error semántico**: no se puede sumar `contador` (NUMÉRICO) con `letra` (CARÁCTER), aunque la instrucción esté perfectamente bien escrita.

## Limitaciones
- El programa no ejecuta ni simula los valores reales de las variables o registros; únicamente valida sus **tipos**, no sus valores.
- El lenguaje reconocido es un subconjunto reducido del ensamblador real del 8086, con fines didácticos: no incluye instrucciones de transferencia (MOV), saltos condicionales, direccionamiento de memoria, ni registros de 16 bits.
- Solo se manejan dos tipos de datos (NUMÉRICO y CARÁCTER); no existen arreglos, estructuras ni otros tamaños de dato.
- Los identificadores no distinguen mayúsculas de minúsculas (se normalizan automáticamente a mayúsculas).
- El árbol de expresión se genera de forma independiente por cada instrucción; no se construye un único árbol sintáctico para todo el programa.

## Conclusión
El desarrollo de este analizador permitió aplicar de manera concreta los conceptos teóricos de las tres primeras fases de un compilador. En particular, quedó demostrado que la validez de un programa no depende únicamente de que las palabras y la estructura sean correctas (léxico y sintáctico), sino también de que su significado sea coherente (semántico): una instrucción como `ADD contador, letra` puede estar perfectamente bien escrita y aun así ser inválida, porque mezcla tipos de datos incompatibles. Implementar cada fase como un autómata manual, sin herramientas de reconocimiento de patrones ya construidas, reforzó además la comprensión de cómo funcionan internamente estas herramientas que normalmente se dan por sentadas en los lenguajes de programación modernos.
 
