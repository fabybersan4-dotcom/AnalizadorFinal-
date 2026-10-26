# Analizador Final
Lenguajes y Autómatas II Grupo: 6N1
Docente: ECHEVERRIA RODRÍGUEZ CESAR OSVALDO
FECHA: 08/09/2026
# Integrantes del Equipo
Fabiola Bernal Sanchez
Emmanuel Cardoza Esquivel
Omar Orozco
Angel Gabriel Gutiérrez Padilla
# Numero de Equipo 
2
## Analizador Léxico, Sintactico y Semántico
Programa en Python con interfaz gráfica que analiza expresiones aritméticas, asignaciones e instrucciones simples de ensamblador, y muestra el resultado de cada fase en una pestaña.

## Descripcion
La aplicación (clase `AnalizadorDashboard`) recibe en un cuadro de texto una expresión o instrucción por línea. Al presionar **Ejecutar análisis**, cada línea pasa por el análisis léxico, el sintáctico y el semántico. Después se genera el árbol de la expresión (gráfico y en jerarquía), el código intermedio de tres direcciones y los cuádruplos. Los errores se marcan en rojo con su diagnóstico y la barra inferior resume cuántos errores léxicos, sintácticos y semánticos se encontraron.

## Objetivo
Implementar las fases de análisis de un compilador (léxica, sintáctica y semántica) y la generación de código intermedio para expresiones aritméticas e instrucciones de ensamblador, mostrando cada resultado de forma visual.

## Tecnologías Utilizadas
- **Python 3**
- **tkinter** (`tk`, `ttk`, `scrolledtext`): ventana, botones, pestañas (`Notebook`), tablas y árbol (`Treeview`) y el `Canvas` donde se dibuja el árbol.
- **re**: expresiones regulares para separar los tokens y validar los operandos.

## Instrucciones Reconocidas

| Tipo | Elementos |
|------|-----------|
| Ensamblador con dos operandos (`OP DESTINO, ORIGEN`) | `MOV`, `ADD`, `SUB`, `MUL`, `DIV`, `CMP`, `AND`, `OR`, `XOR`, `SHL`, `SHR` |
| Ensamblador con un operando (`OP DESTINO`) | `INC`, `DEC`, `NEG`, `NOT` |
| Operadores aritméticos | `+`  `-`  `*`  `/`  `%`  `^` |
| Otros símbolos | `=` (asignación), `(` `)` (paréntesis), `,` (coma) |

## Registros Desconocidos
Los registros reconocidos son:
- 16 bits: `AX`, `BX`, `CX`, `DX`, `SI`, `DI`
- 8 bits: `AL`, `AH`, `BL`, `CL`

Cualquier otro nombre (por ejemplo `DL`, `BH`, `SP`) no se reconoce como registro: el análisis léxico lo clasifica como `IDENTIFICADOR` y el semántico no valida su tamaño.

## Análisis Léxico
Divide cada línea en lexemas con una expresión regular (`[a-zA-Z0-9_]+`, símbolos `+ - * / % ^ = ( ) ,` o cualquier otro carácter) y clasifica cada uno:

`OP_ARITMETICO`, `ASIGNACION`, `PARENTESIS`, `COMA`, `VALOR_INMEDIATO`, `OP_ENSAMBLADOR`, `REGISTRO_GENERAL`, `IDENTIFICADOR`.

Si un lexema no encaja en ninguna clase (por ejemplo `@` o `2abc`), se marca como **ERROR LÉXICO**. La tabla guarda la línea, la columna, el lexema y su diagnóstico.

## Análisis Sintactico
- **Instrucciones de ensamblador:** se verifica el formato `OP DESTINO` (2 elementos) o `OP DESTINO, ORIGEN` (4 elementos, con la coma en tercer lugar) y que los operandos sean registro, identificador o número.
- **Expresiones y asignaciones:** un parser por descenso recursivo construye el árbol (AST) con esta gramática, de menor a mayor precedencia:

```
sentencia -> expr [ '=' sentencia ]
expr      -> termino { ('+' | '-') termino }
termino   -> unario  { ('*' | '/' | '%') unario }
unario    -> '-' unario | potencia
potencia  -> factor [ '^' unario ]
factor    -> '(' expr ')' | IDENTIFICADOR | NUMERO
```

Si una línea ya tiene error léxico, el sintáctico se marca como `ERROR ENCADENADO` y no se analiza.

## Análisis Semántico
**Ensamblador:**
- El destino no puede ser un número.
- `SHL` y `SHR` necesitan un valor inmediato que no supere el tamaño del registro.
- `DIV` entre cero es error.
- No se pueden mezclar registros de 8 y 16 bits (`Incompatibilidad: 16-bits vs 8-bits`).
- Un valor inmediato no puede pasar de 255 (registros de 8 bits) ni de 65535 (registros de 16 bits).

**Expresiones:** se detecta la división o el módulo entre un cero escrito directamente (`/ 0`, `% 0`).

Si la línea tiene error léxico o sintáctico, el semántico se marca como `ERROR ENCADENADO`.

## Resultados
Al ejecutar el análisis se llenan siete pestañas:

| Pestaña | Contenido |
|---------|-----------|
| 1. Léxico | Tabla de lexemas con su diagnóstico |
| 2. Sintáctico | Estado y detalle por línea |
| 3. Semántico | Estado y detalle por línea |
| 4. Árbol | Árbol dibujado en horizontal (hojas a la izquierda, raíz a la derecha) |
| 5. Jerarquía | El mismo árbol (AST) en forma de lista desplegable |
| 6. Cód. Interm. | Código de tres direcciones |
| 7. Cuádruplos | Tabla (operador, argumento 1, argumento 2, resultado) |

Los ejemplos de las siguientes secciones usan esta entrada:

```
a = b + 3 * c
MOV AX, 5
ADD AX, BX
MUL AX, 2
```

## Resulado Léxico
Para la línea 1 (`a = b + 3 * c`):

| Línea | Col. | Lexema | Diagnóstico |
|-------|------|--------|-------------|
| 1 | 1 | a | IDENTIFICADOR |
| 1 | 3 | = | ASIGNACION |
| 1 | 5 | b | IDENTIFICADOR |
| 1 | 7 | + | OP_ARITMETICO |
| 1 | 9 | 3 | VALOR_INMEDIATO |
| 1 | 11 | * | OP_ARITMETICO |
| 1 | 13 | c | IDENTIFICADOR |

Con un símbolo no permitido, por ejemplo `x = 5 @ 3`, el lexema `@` aparece en rojo como `[ERROR LÉXICO] Símbolo no permitido ('@')`.

## Resultado Sintactico

| Línea | Instrucción | Diagnóstico | Detalle |
|-------|-------------|-------------|---------|
| 1 | a = b + 3 * c | Sintaxis Válida | Asignación gramaticalmente correcta (ID = expresión) |
| 2 | MOV AX, 5 | Sintaxis Válida | Estructura Correcta (MOV DESTINO, ORIGEN) |

Con `MOV AX BX` (sin coma) aparece en rojo: `[ERROR SINTÁCTICO] Formato incorrecto. Esperado: MOV DESTINO, ORIGEN`.

## Resultado Semantico

| Línea | Instrucción | Diagnóstico | Detalle |
|-------|-------------|-------------|---------|
| 1 | a = b + 3 * c | Semántica Válida | Asignación a 'a' con operadores coherentes |
| 2 | MOV AX, 5 | Semántica Válida | Lógica y tipos compatibles |

Con `MOV AX, BL` aparece `[ERROR SEMÁNTICO] Incompatibilidad: 16-bits vs 8-bits`, y con `MOV AL, 300`, `Valor inmediato superior a 8 bits (máx 255)`.

## Estructura del proyecto

```
Analizador/
├── analizador.py
└── README.md
```

## Archivos Principales

| Archivo / clase | Función |
|-----------------|---------|
| `analizador.py` | Programa completo (un solo archivo) |
| `NodoAST` | Nodo del árbol: valor, tipo de nodo, tipo semántico, hijo izquierdo y derecho |
| `GeneradorCodigoIntermedio` | Recorre el árbol en postorden y genera cuádruplos y código de tres direcciones |
| `ConstructorArbolASM` | Une todas las instrucciones de ensamblador válidas en un solo árbol de flujo de datos |
| `AnalizadorDashboard` | Interfaz gráfica, análisis léxico, parser, validación semántica y dibujo del árbol |

## Ejecucion
Requiere Python 3 con tkinter (incluido en el instalador de Python para Windows).

```
python analizador.py
```

## Compilacion
No requiere compilación: Python es un lenguaje interpretado y el programa se ejecuta directamente con el comando anterior.

## Funcionamiento
1. Escribir una expresión o instrucción por línea en el cuadro **Código / Expresiones de entrada**.
2. Presionar **Ejecutar análisis**.
3. Cada línea no vacía se divide en lexemas y se clasifica (fase léxica).
4. Si no hay error léxico, se valida su estructura (fase sintáctica) y después su significado (fase semántica).
5. Las expresiones válidas generan su árbol, su código de tres direcciones y sus cuádruplos. Los temporales (`t1`, `t2`, ...) se reinician en cada línea.
6. Las instrucciones de ensamblador válidas se combinan en un solo árbol al final de los resultados.
7. La barra inferior muestra el total de errores léxicos, sintácticos y semánticos.
8. **Limpiar Todo** borra la entrada y los resultados.

## Ejemplo
Con la entrada:

```
a = b + 3 * c
MOV AX, 5
ADD AX, BX
MUL AX, 2
```

Código de tres direcciones:

| Línea | # | Código |
|-------|---|--------|
| 1 | 1 | t1 = 3 * c |
| 1 | 2 | t2 = b + t1 |
| 1 | 3 | a = t2 |
| 2 | 4 | AX = 5 |
| 3 | 5 | AX = AX + BX |
| 4 | 6 | AX = AX * 2 |

Cuádruplos:

| Línea | # | Operador | Argumento 1 | Argumento 2 | Resultado |
|-------|---|----------|-------------|-------------|-----------|
| 1 | 1 | * | 3 | c | t1 |
| 1 | 2 | + | b | t1 | t2 |
| 1 | 3 | = | t2 | — | a |
| 2 | 4 | MOV | 5 | — | AX |
| 3 | 5 | ADD | AX | BX | AX |
| 4 | 6 | MUL | AX | 2 | AX |

Las tres instrucciones de ensamblador se unen en un árbol que representa `AX = (5 + BX) * 2`.

## Limitaciones
- Solo se reconocen los registros `AX`, `BX`, `CX`, `DX`, `SI`, `DI`, `AL`, `AH`, `BL` y `CL`.
- Solo se manejan números enteros en decimal (sin decimales ni hexadecimal).
- No hay etiquetas, saltos ni acceso a memoria.
- La división entre cero solo se detecta cuando el divisor es un `0` escrito directamente, no una variable con valor cero.
- No se valida que los identificadores estén declarados.
- Si un nodo del árbol de ensamblador supera 40 nodos, se representa como referencia (`[REG]`) en lugar de copiarlo.

## Conclusion
El programa integra en una sola herramienta las fases léxica, sintáctica y semántica de un compilador, y genera el árbol, el código de tres direcciones y los cuádruplos de cada entrada. Mostrar cada resultado en su propia pestaña permite ver cómo avanza el análisis y dónde se origina cada error.
