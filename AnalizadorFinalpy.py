import tkinter as tk
from tkinter import ttk
from tkinter import scrolledtext
import re


def es_numero(txt):
    """True solo para enteros formados por dígitos ASCII (0-9)."""
    return bool(re.fullmatch(r'[0-9]+', txt))


class NodoAST:
    def __init__(self, valor, tipo_nodo, tipo_semantico=None, izq=None, der=None):
        self.valor = valor
        self.tipo_nodo = tipo_nodo
        self.tipo_semantico = tipo_semantico
        self.izq = izq
        self.der = der
        self.x = 0
        self.y = 0


class GeneradorCodigoIntermedio:
    """Recorre el AST en postorden y produce cuádruplos (operador, arg1, arg2, resultado).

    Los temporales (t1, t2, ...) se reinician en cada línea de código.
    """

    SIMBOLOS_ASM = {"ADD": "+", "SUB": "-", "MUL": "*", "DIV": "/", "SHL": "<<", "SHR": ">>"}

    def __init__(self):
        self.temp = 0
        self.cuadruplos = []

    def nuevo_temporal(self):
        self.temp += 1
        return f"t{self.temp}"

    def generar(self, nodo):
        """Devuelve el 'lugar' (variable, número o temporal) donde queda el valor del nodo."""
        if nodo.tipo_nodo == "OPERANDO":
            return nodo.valor

        if nodo.tipo_nodo == "OP_UNARIO":
            a = self.generar(nodo.izq)
            t = self.nuevo_temporal()
            self.cuadruplos.append(("minus", a, "", t))
            return t

        if nodo.tipo_nodo == "OP_ARITMETICO":
            a = self.generar(nodo.izq)
            b = self.generar(nodo.der)
            t = self.nuevo_temporal()
            self.cuadruplos.append((nodo.valor, a, b, t))
            return t

        if nodo.tipo_nodo == "ASIGNACION":
            r = self.generar(nodo.der)
            destino = nodo.izq.valor
            self.cuadruplos.append(("=", r, "", destino))
            return destino

        if nodo.tipo_nodo == "INSTRUCCION":
            op = nodo.valor
            destino = nodo.izq.valor
            origen = nodo.der.valor if nodo.der else ""
            if op == "MOV":
                self.cuadruplos.append(("MOV", origen, "", destino))
            elif op == "CMP":
                self.cuadruplos.append(("CMP", destino, origen, ""))
            else:
                self.cuadruplos.append((op, destino, origen, destino))
            return destino

        return None

    @staticmethod
    def a_tres_direcciones(cuad):
        op, a1, a2, res = cuad
        if op in ("=", "MOV"):
            return f"{res} = {a1}"
        if op in ("minus", "NEG"):
            return f"{res} = -{a1}"
        if op == "NOT":
            return f"{res} = NOT {a1}"
        if op == "INC":
            return f"{res} = {a1} + 1"
        if op == "DEC":
            return f"{res} = {a1} - 1"
        if op == "CMP":
            return f"CMP {a1}, {a2}"
        simbolo = GeneradorCodigoIntermedio.SIMBOLOS_ASM.get(op, op)
        return f"{res} = {a1} {simbolo} {a2}"


class ConstructorArbolASM:
    """Une TODAS las instrucciones de ensamblador en un solo árbol (flujo de datos).

    Cada registro guarda el árbol de la expresión que calcula su valor actual:
        MOV AX, 5 / ADD AX, BX / MUL AX, 2   ->   AX = (5 + BX) * 2
    Si se escriben varios registros, las asignaciones se enlazan con nodos ';'.
    CMP actualiza un registro especial llamado FLAGS.
    """

    SIMBOLOS = {"ADD": "+", "SUB": "-", "MUL": "*", "DIV": "/", "SHL": "<<", "SHR": ">>"}
    MAX_NODOS_COPIA = 40  # evita que ADD AX, AX repetido duplique el árbol sin límite

    def __init__(self):
        self.estado = {}   # registro -> nodo con su valor actual
        self.orden = []    # registros escritos, en orden de primera escritura

    @staticmethod
    def contar(nodo):
        if nodo is None:
            return 0
        return 1 + ConstructorArbolASM.contar(nodo.izq) + ConstructorArbolASM.contar(nodo.der)

    @staticmethod
    def copiar(nodo):
        if nodo is None:
            return None
        return NodoAST(nodo.valor, nodo.tipo_nodo, nodo.tipo_semantico,
                       ConstructorArbolASM.copiar(nodo.izq), ConstructorArbolASM.copiar(nodo.der))

    def valor_de(self, operando):
        """Árbol que representa el valor de un operando (copia, para no compartir nodos)."""
        if es_numero(operando):
            return NodoAST(operando, "OPERANDO", "VALOR_INMEDIATO")
        actual = self.estado.get(operando)
        if actual is None:
            return NodoAST(operando, "OPERANDO", "VALOR_INICIAL")
        if self.contar(actual) > self.MAX_NODOS_COPIA:
            return NodoAST(f"[{operando}]", "OPERANDO", "REFERENCIA")
        return self.copiar(actual)

    def _asignar(self, nombre, nodo):
        if nombre not in self.orden:
            self.orden.append(nombre)
        self.estado[nombre] = nodo

    def aplicar(self, op, destino, origen):
        if op == "CMP":
            izq = self.valor_de(destino)
            der = self.valor_de(origen)
            self._asignar("FLAGS", NodoAST("CMP", "OP_ARITMETICO", "COMPARACION", izq=izq, der=der))
            return
        if op == "MOV":
            self._asignar(destino, self.valor_de(origen))
            return

        # El valor actual del destino pasa a ser hijo del nuevo nodo (se mueve, no se copia)
        fuente = self.valor_de(origen) if origen else None
        actual = self.estado.get(destino)
        if actual is None:
            actual = NodoAST(destino, "OPERANDO", "VALOR_INICIAL")

        if op == "INC":
            nuevo = NodoAST("+", "OP_ARITMETICO", "OPERACION_ARITMETICA", izq=actual,
                            der=NodoAST("1", "OPERANDO", "VALOR_INMEDIATO"))
        elif op == "DEC":
            nuevo = NodoAST("-", "OP_ARITMETICO", "OPERACION_ARITMETICA", izq=actual,
                            der=NodoAST("1", "OPERANDO", "VALOR_INMEDIATO"))
        elif op == "NEG":
            nuevo = NodoAST("neg", "OP_UNARIO", "MENOS_UNARIO", izq=actual)
        elif op == "NOT":
            nuevo = NodoAST("NOT", "OP_UNARIO", "NEGACION_LOGICA", izq=actual)
        else:
            nuevo = NodoAST(self.SIMBOLOS.get(op, op), "OP_ARITMETICO", "OPERACION_" + op, izq=actual, der=fuente)
        self._asignar(destino, nuevo)

    def construir(self, instrucciones):
        """instrucciones: lista de tuplas (num_linea, op, destino, origen)."""
        for _, op, destino, origen in instrucciones:
            self.aplicar(op, destino, origen)

        asignaciones = [NodoAST("=", "ASIGNACION", "ASIGNACION",
                                izq=NodoAST(nombre, "OPERANDO", "DESTINO"), der=self.estado[nombre])
                        for nombre in self.orden]
        raiz = asignaciones[-1]
        for asig in reversed(asignaciones[:-1]):
            raiz = NodoAST(";", "SECUENCIA", "SECUENCIA", izq=asig, der=raiz)
        return raiz


class AnalizadorDashboard(tk.Tk):
    # Tamaño del dibujo del árbol
    RADIO_NODO = 24
    CELDA_X = 125
    CELDA_Y = 64
    FUENTE_NODO = ("Consolas", 13, "bold")

    def __init__(self):
        super().__init__()

        self.title("Analizador Léxico, Sintáctico y Semántico con Árbol Horizontal y Código Intermedio")
        self.geometry("1360x820")
        self.configure(bg="#D4E2F0")
        self.resizable(True, True)

        self.style = ttk.Style()
        self.style.theme_use("clam")

        self.style.configure("Treeview.Heading", font=("Segoe UI", 10, "bold"),
                             background="#FFFFFF", foreground="#333333", relief="flat")
        self.style.configure("Treeview", font=("Consolas", 10), rowheight=24,
                             background="#FFFFFF", fieldbackground="#FFFFFF", foreground="#333333")
        self.style.map("Treeview", background=[("selected", "#0078D7")], foreground=[("selected", "white")])

        self.definir_lenguaje()
        self.crear_interfaz()

    def definir_lenguaje(self):
        self.regs_16bit = {"AX", "BX", "CX", "DX", "SI", "DI"}
        self.regs_8bit = {"AL", "AH", "BL", "CL"}
        self.asm_binarias = {"MOV", "ADD", "SUB", "MUL", "DIV", "CMP", "AND", "OR", "XOR", "SHL", "SHR"}
        self.asm_unarias = {"INC", "DEC", "NEG", "NOT"}
        self.operaciones_asm = self.asm_binarias | self.asm_unarias
        self.ops_aritmeticos = {"+", "-", "*", "/", "%", "^"}

    def crear_interfaz(self):
        # Encabezado
        frame_superior = tk.Frame(self, bg="#0F3D7A", height=55)
        frame_superior.pack(fill=tk.X, side=tk.TOP)
        frame_superior.pack_propagate(False)

        lbl_titulo = tk.Label(frame_superior, text="⚡ PANEL DE CONTROL - COMPILADOR Y GENERADOR DE ÁRBOLES",
                              font=("Segoe UI", 15, "bold"), fg="white", bg="#0F3D7A")
        lbl_titulo.pack(side=tk.LEFT, padx=20, pady=10)

        # Barra de estado inferior
        self.frame_status = tk.Frame(self, bg="#E6EEF4", height=50, relief="solid", bd=1)
        self.frame_status.pack(fill=tk.X, side=tk.BOTTOM, padx=15, pady=(0, 10))
        self.frame_status.pack_propagate(False)

        self.lblStatus = tk.Label(self.frame_status, text="Esperando entrada...", font=("Segoe UI", 11, "bold"), bg="#E6EEF4", fg="#333333")
        self.lblStatus.pack(pady=(2, 1))
        self.lblDetalle = tk.Label(self.frame_status, text="Ingresa expresiones o instrucciones y presiona Analizar", font=("Segoe UI", 9), bg="#E6EEF4", fg="#666666")
        self.lblDetalle.pack()

        frame_cuerpo = tk.Frame(self, bg="#D4E2F0")
        frame_cuerpo.pack(fill=tk.BOTH, expand=True, padx=15, pady=8)

        # Columna Izquierda (Entrada)
        self.col_izquierda = tk.Frame(frame_cuerpo, bg="#D4E2F0", width=350)
        self.col_izquierda.pack(side=tk.LEFT, fill=tk.BOTH, padx=(0, 10))
        self.col_izquierda.pack_propagate(False)

        panel_codigo = tk.LabelFrame(self.col_izquierda, text="📄 CÓDIGO / EXPRESIONES DE ENTRADA",
                                     font=("Segoe UI", 11, "bold"), bg="#FFFFFF", fg="#1E3D59", relief="solid", bd=1)
        panel_codigo.pack(fill=tk.BOTH, expand=True)

        self.txtCodigo = scrolledtext.ScrolledText(panel_codigo, font=("Consolas", 12), bg="#FFFFFF", fg="#333333", bd=0)
        self.txtCodigo.pack(fill=tk.BOTH, expand=True, padx=8, pady=8)

        self.btnAnalizarIzq = tk.Button(self.col_izquierda, text="▶  EJECUTAR ANÁLISIS", command=self.analizar,
                                        font=("Segoe UI", 11, "bold"), bg="#007ACC", fg="white", cursor="hand2", bd=0)
        self.btnAnalizarIzq.pack(fill=tk.X, pady=(8, 4), ipady=6)

        self.btnLimpiar = tk.Button(self.col_izquierda, text="🗑️ Limpiar Todo", command=self.limpiar,
                                    font=("Segoe UI", 10), bg="#FFFFFF", fg="#333333", cursor="hand2", relief="solid", bd=1)
        self.btnLimpiar.pack(fill=tk.X, pady=(0, 0), ipady=4)

        # Columna Derecha (Pestañas)
        self.col_derecha = tk.Frame(frame_cuerpo, bg="#D4E2F0")
        self.col_derecha.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        self.btnAnalizar = tk.Button(self.col_derecha, text="▶  EJECUTAR ANÁLISIS COMPLETO (LÉXICO, SINTÁCTICO, SEMÁNTICO, ÁRBOL Y CÓDIGO)",
                                     command=self.analizar, font=("Segoe UI", 11, "bold"), bg="#00A8CC", fg="white",
                                     cursor="hand2", bd=0)
        self.btnAnalizar.pack(fill=tk.X, side=tk.TOP, pady=(0, 8), ipady=7)

        self.notebook = ttk.Notebook(self.col_derecha)
        self.notebook.pack(fill=tk.BOTH, expand=True)

        # --- 1. Pestaña Léxico ---
        self.tab_lex = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_lex, text="📝 1. Léxico")
        self.tabla_lex = self.crear_tabla(self.tab_lex, ("linea", "columna", "lexema", "tipo"),
                                          ["Línea", "Col.", "Lexema", "Diagnóstico Léxico"], [60, 60, 130, 320])
        self.tabla_lex.tag_configure('error', background='#FFCDD2', foreground='#B71C1C')

        # --- 2. Pestaña Sintáctico ---
        self.tab_sin = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_sin, text="🏗️ 2. Sintáctico")
        self.tabla_sin = self.crear_tabla(self.tab_sin, ("linea", "instruccion", "estado", "detalle"),
                                          ["Línea", "Instrucción / Expresión", "Diagnóstico", "Detalle Sintáctico"], [60, 200, 150, 290])
        self.tabla_sin.tag_configure('error', background='#FFCDD2', foreground='#B71C1C')
        self.tabla_sin.tag_configure('ok', background='#C8E6C9', foreground='#1B5E20')

        # --- 3. Pestaña Semántico ---
        self.tab_sem = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_sem, text="🧠 3. Semántico")
        self.tabla_sem = self.crear_tabla(self.tab_sem, ("linea", "instruccion", "estado", "detalle"),
                                          ["Línea", "Instrucción / Expresión", "Diagnóstico", "Detalle Semántico"], [60, 200, 150, 290])
        self.tabla_sem.tag_configure('error', background='#FFCDD2', foreground='#B71C1C')
        self.tabla_sem.tag_configure('ok', background='#C8E6C9', foreground='#1B5E20')
        self.tabla_sem.tag_configure('omitido', background='#FFF9C4', foreground='#F57F17')

        # --- 4. Pestaña Árbol Gráfico Horizontal Estilo Libreta ---
        self.tab_arbol_grafico = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_arbol_grafico, text="📐 4. Árbol")

        frame_canvas = tk.Frame(self.tab_arbol_grafico, bg="#FFFFFF")
        frame_canvas.pack(fill=tk.BOTH, expand=True)

        self.scroll_x = tk.Scrollbar(frame_canvas, orient=tk.HORIZONTAL)
        self.scroll_y = tk.Scrollbar(frame_canvas, orient=tk.VERTICAL)
        self.canvas = tk.Canvas(frame_canvas, bg="#FFFFFF",
                                xscrollcommand=self.scroll_x.set, yscrollcommand=self.scroll_y.set)
        self.scroll_x.config(command=self.canvas.xview)
        self.scroll_y.config(command=self.canvas.yview)

        self.scroll_y.pack(side=tk.RIGHT, fill=tk.Y)
        self.scroll_x.pack(side=tk.BOTTOM, fill=tk.X)
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # --- 5. Pestaña Jerarquía AST ---
        self.tab_ast = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_ast, text="🌳 5. Jerarquía")
        self.arbol_ast = ttk.Treeview(self.tab_ast, columns=("tipo", "valor", "estado"), show="tree headings", selectmode="browse")
        self.arbol_ast.heading("#0", text="Jerarquía de Nodos (AST)", anchor=tk.W)
        self.arbol_ast.heading("tipo", text="Tipo Semántico", anchor=tk.CENTER)
        self.arbol_ast.heading("valor", text="Elemento", anchor=tk.CENTER)
        self.arbol_ast.heading("estado", text="Diagnóstico", anchor=tk.W)
        self.arbol_ast.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        self.arbol_ast.tag_configure('raiz', font=("Consolas", 10, "bold"), background='#E3F2FD')

        # --- 6. Pestaña Código Intermedio (tres direcciones) ---
        self.tab_ci = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_ci, text="⚙️ 6. Cód. Interm.")
        self.tabla_ci = self.crear_tabla(self.tab_ci, ("linea", "idx", "codigo"),
                                         ["Línea", "#", "Código de tres direcciones"], [60, 50, 500])
        self._configurar_tags_codigo(self.tabla_ci)

        # --- 7. Pestaña Cuádruplos ---
        self.tab_cuad = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_cuad, text="📊 7. Cuádruplos")
        self.tabla_cuad = self.crear_tabla(self.tab_cuad, ("linea", "idx", "op", "arg1", "arg2", "res"),
                                           ["Línea", "#", "Operador", "Argumento 1", "Argumento 2", "Resultado"],
                                           [60, 50, 110, 140, 140, 140])
        self._configurar_tags_codigo(self.tabla_cuad)

    def _configurar_tags_codigo(self, tabla):
        tabla.tag_configure('par', background='#FFFFFF')
        tabla.tag_configure('impar', background='#EEF4FB')
        tabla.tag_configure('error', background='#FFCDD2', foreground='#B71C1C')
        tabla.tag_configure('info', background='#FFF9C4', foreground='#F57F17')

    def crear_tabla(self, parent, columnas, titulos, anchos):
        tabla = ttk.Treeview(parent, columns=columnas, show="headings", selectmode="browse")
        for col, titulo, ancho in zip(columnas, titulos, anchos):
            align = tk.W if col in ("tipo", "detalle", "instruccion", "codigo") else tk.CENTER
            tabla.heading(col, text=titulo, anchor=align)
            tabla.column(col, width=ancho, anchor=align)
        scroll = ttk.Scrollbar(parent, orient=tk.VERTICAL, command=tabla.yview)
        tabla.configure(yscrollcommand=scroll.set)
        scroll.pack(side=tk.RIGHT, fill=tk.Y)
        tabla.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        return tabla

    # --- PARSER POR DESCENSO RECURSIVO ---
    # Gramática (de menor a mayor precedencia):
    #   sentencia -> expr [ '=' sentencia ]          (asignación, asociativa a la derecha)
    #   expr      -> termino { ('+' | '-') termino }
    #   termino   -> unario  { ('*' | '/' | '%') unario }
    #   unario    -> '-' unario | potencia
    #   potencia  -> factor [ '^' unario ]           (asociativa a la derecha)
    #   factor    -> '(' expr ')' | IDENTIFICADOR | NUMERO
    def parse_expresion(self, tokens):
        self.tokens_expr = tokens
        self.pos_expr = 0
        raiz = self.sentencia()
        if self.pos_expr < len(self.tokens_expr):
            raise Exception(f"Símbolo inesperado '{self.tokens_expr[self.pos_expr]}'")
        return raiz

    def token_actual(self):
        return self.tokens_expr[self.pos_expr] if self.pos_expr < len(self.tokens_expr) else None

    def consumir(self, esperado=None):
        tok = self.token_actual()
        if tok is None:
            raise Exception("Fin inesperado de expresión")
        if esperado and tok != esperado:
            raise Exception(f"Se esperaba '{esperado}' y se encontró '{tok}'")
        self.pos_expr += 1
        return tok

    def sentencia(self):
        nodo = self.expr()
        if self.token_actual() == "=":
            if nodo.tipo_nodo != "OPERANDO" or nodo.tipo_semantico != "IDENTIFICADOR":
                raise Exception("El lado izquierdo de '=' debe ser un identificador")
            self.consumir("=")
            der = self.sentencia()
            return NodoAST("=", "ASIGNACION", "ASIGNACION", izq=nodo, der=der)
        return nodo

    def expr(self):
        nodo = self.termino()
        while self.token_actual() in ("+", "-"):
            op = self.consumir()
            der = self.termino()
            nodo = NodoAST(op, "OP_ARITMETICO", "OPERACION_ARITMETICA", izq=nodo, der=der)
        return nodo

    def termino(self):
        nodo = self.unario()
        while self.token_actual() in ("*", "/", "%"):
            op = self.consumir()
            der = self.unario()
            nodo = NodoAST(op, "OP_ARITMETICO", "OPERACION_ARITMETICA", izq=nodo, der=der)
        return nodo

    def unario(self):
        if self.token_actual() == "-":
            self.consumir("-")
            operando = self.unario()
            return NodoAST("neg", "OP_UNARIO", "MENOS_UNARIO", izq=operando)
        return self.potencia()

    def potencia(self):
        base = self.factor()
        if self.token_actual() == "^":
            op = self.consumir()
            exponente = self.unario()
            return NodoAST(op, "OP_ARITMETICO", "OPERACION_ARITMETICA", izq=base, der=exponente)
        return base

    def factor(self):
        tok = self.token_actual()
        if tok is None:
            raise Exception("Fin inesperado de expresión")
        if tok == "(":
            self.consumir("(")
            nodo = self.expr()
            self.consumir(")")
            return nodo
        elif re.fullmatch(r'[A-Za-z0-9_]+', tok):
            self.consumir()
            tipo = "VALOR_INMEDIATO" if es_numero(tok) else "IDENTIFICADOR"
            return NodoAST(tok, "OPERANDO", tipo)
        else:
            raise Exception(f"Sintaxis inválida cerca de '{tok}'")

    # --- SEMÁNTICA ---
    def buscar_division_cero(self, nodo):
        if nodo is None:
            return None
        if (nodo.tipo_nodo == "OP_ARITMETICO" and nodo.valor in ("/", "%")
                and nodo.der and nodo.der.tipo_nodo == "OPERANDO"
                and es_numero(nodo.der.valor) and int(nodo.der.valor) == 0):
            return f"División entre cero con el operador '{nodo.valor}'"
        return self.buscar_division_cero(nodo.izq) or self.buscar_division_cero(nodo.der)

    def tam_registro(self, reg):
        if reg in self.regs_16bit:
            return 16
        if reg in self.regs_8bit:
            return 8
        return None

    def validar_semantica_asm(self, op, destino, origen):
        """Devuelve (estado, detalle, tag) para una instrucción de ensamblador ya válida sintácticamente."""
        ERR = "[ERROR SEMÁNTICO]"
        if es_numero(destino):
            return ERR, "El destino de la operación no puede ser un número", "error"

        s1 = self.tam_registro(destino)

        if op in self.asm_unarias:
            return "Semántica Válida", "Operando único válido", "ok"

        if op in ("SHL", "SHR"):
            if not es_numero(origen):
                return ERR, "El contador de desplazamiento debe ser un valor inmediato", "error"
            if int(origen) > (s1 or 16):
                return ERR, f"Desplazamiento mayor al tamaño del registro ({s1 or 16} bits)", "error"
            return "Semántica Válida", "Desplazamiento válido", "ok"

        if op == "DIV" and es_numero(origen) and int(origen) == 0:
            return ERR, "División entre cero", "error"

        if not es_numero(origen):
            s2 = self.tam_registro(origen)
            if s1 and s2 and s1 != s2:
                return ERR, f"Incompatibilidad: {s1}-bits vs {s2}-bits", "error"
        else:
            val = int(origen)
            if s1 == 8 and val > 255:
                return ERR, "Valor inmediato superior a 8 bits (máx 255)", "error"
            if s1 == 16 and val > 65535:
                return ERR, "Valor inmediato superior a 16 bits (máx 65535)", "error"

        return "Semántica Válida", "Lógica y tipos compatibles", "ok"

    # --- GEOMETRÍA HORIZONTAL (HOJAS IZQUIERDA ➔ RAÍZ DERECHA) ---
    def calcular_posiciones(self, nodo, nivel):
        if nodo is None:
            return
        nodo.x = nivel
        hijos = [h for h in (nodo.izq, nodo.der) if h is not None]
        if not hijos:
            nodo.y = self.hoja_y_actual
            self.hoja_y_actual += 1
        else:
            for h in hijos:
                self.calcular_posiciones(h, nivel - 1)
            nodo.y = sum(h.y for h in hijos) / len(hijos)

    def obtener_profundidad(self, nodo):
        if nodo is None:
            return 0
        return 1 + max(self.obtener_profundidad(nodo.izq), self.obtener_profundidad(nodo.der))

    def semiancho_nodo(self, nodo):
        """Los nodos con texto largo se ensanchan para que el texto quede dentro del óvalo."""
        return max(self.RADIO_NODO, int(6.5 * len(str(nodo.valor))) + 10)

    def dibujar_arbol_en_canvas(self, nodo, offset_x, offset_y, celda_x, celda_y):
        if nodo is None:
            return

        x_actual = offset_x + nodo.x * celda_x
        y_actual = offset_y + nodo.y * celda_y
        rx = self.semiancho_nodo(nodo)
        ry = self.RADIO_NODO

        for hijo in (nodo.izq, nodo.der):
            if hijo:
                hx = offset_x + hijo.x * celda_x
                hy = offset_y + hijo.y * celda_y
                self.canvas.create_line(x_actual - rx, y_actual, hx + self.semiancho_nodo(hijo), hy,
                                        fill="#1565C0", width=2)
                self.dibujar_arbol_en_canvas(hijo, offset_x, offset_y, celda_x, celda_y)

        if nodo.tipo_nodo == "ASIGNACION":
            color_fondo, color_borde = "#C8E6C9", "#1B5E20"
        elif nodo.tipo_nodo == "SECUENCIA":
            color_fondo, color_borde = "#ECEFF1", "#455A64"
        elif nodo.izq or nodo.der:
            color_fondo, color_borde = "#E3F2FD", "#0D47A1"
        else:
            color_fondo, color_borde = "#FFF9C4", "#F57F17"

        self.canvas.create_oval(x_actual - rx, y_actual - ry, x_actual + rx, y_actual + ry,
                                fill=color_fondo, outline=color_borde, width=2)
        self.canvas.create_text(x_actual, y_actual, text=str(nodo.valor),
                                font=self.FUENTE_NODO, fill="#111111")

    def agregar_arbol_al_canvas(self, titulo, raiz, y_cursor, max_x):
        """Dibuja un árbol con su título debajo del anterior. Devuelve (nuevo_y, nuevo_max_x)."""
        self.canvas.create_text(30, y_cursor, text=titulo, anchor="w",
                                font=("Segoe UI", 12, "bold"), fill="#0F3D7A")
        y_cursor += 40

        profundidad = self.obtener_profundidad(raiz)
        self.hoja_y_actual = 0
        self.calcular_posiciones(raiz, nivel=profundidad)

        offset_x = 70
        self.dibujar_arbol_en_canvas(raiz, offset_x, y_cursor, self.CELDA_X, self.CELDA_Y)

        alto_arbol = (self.hoja_y_actual + 1) * self.CELDA_Y
        ancho_arbol = offset_x + (profundidad + 2) * self.CELDA_X
        max_x = max(max_x, ancho_arbol)

        y_cursor += alto_arbol + 30
        self.canvas.create_line(20, y_cursor - 15, max_x, y_cursor - 15, fill="#CFD8DC", dash=(4, 2))
        return y_cursor, max_x

    def dibujar_cuadricula(self, ancho, alto, tam=25):
        for x in range(0, ancho, tam):
            self.canvas.create_line(x, 0, x, alto, fill="#E6EEF8", width=1, tags="grid")
        for y in range(0, alto, tam):
            self.canvas.create_line(0, y, ancho, y, fill="#E6EEF8", width=1, tags="grid")

    def poblar_treeview(self, parent_id, nodo):
        if nodo is None:
            return
        txt = f"{nodo.tipo_nodo}: [{nodo.valor}]"
        nid = self.arbol_ast.insert(parent_id, tk.END, text=txt,
                                    values=(nodo.tipo_semantico, nodo.valor, "Válido"), open=True)
        if nodo.izq: self.poblar_treeview(nid, nodo.izq)
        if nodo.der: self.poblar_treeview(nid, nodo.der)

    def analizar(self):
        for t in [self.tabla_lex, self.tabla_sin, self.tabla_sem, self.arbol_ast, self.tabla_ci, self.tabla_cuad]:
            for item in t.get_children():
                t.delete(item)
        self.canvas.delete("all")

        codigo = self.txtCodigo.get(1.0, tk.END)
        lineas = codigo.split('\n')
        patron_tokens = re.compile(r'[a-zA-Z0-9_]+|[\+\-\*\/\%\^\=\(\)\,]|.')
        operando_valido = re.compile(r'[A-Za-z0-9_]+')

        err_lex, err_sin, err_sem = 0, 0, 0
        y_cursor_canvas = 30
        max_x_alcanzado = 800
        idx_cuad = 0
        instrucciones_asm = []   # (línea, op, destino, origen) de las instrucciones ASM sin errores

        for num_linea, linea in enumerate(lineas, 1):
            linea_str = linea.strip()
            if not linea_str:
                continue

            matches = list(patron_tokens.finditer(linea))
            tokens_linea = []
            hay_error_lexico = False

            # --- 1. ANÁLISIS LÉXICO ---
            for match in matches:
                lexema = match.group()
                if lexema.isspace(): continue
                col = match.start() + 1
                es_err = False

                if lexema in self.ops_aritmeticos: tipo = "OP_ARITMETICO"
                elif lexema == "=": tipo = "ASIGNACION"
                elif lexema in ("(", ")"): tipo = "PARENTESIS"
                elif lexema == ",": tipo = "COMA"
                elif es_numero(lexema): tipo = "VALOR_INMEDIATO"
                elif lexema in self.operaciones_asm: tipo = "OP_ENSAMBLADOR"
                elif lexema in self.regs_16bit or lexema in self.regs_8bit: tipo = "REGISTRO_GENERAL"
                elif re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', lexema): tipo = "IDENTIFICADOR"
                else:
                    tipo = f"[ERROR LÉXICO] Símbolo no permitido ('{lexema}')"
                    es_err = True
                    hay_error_lexico = True
                    err_lex += 1

                tokens_linea.append(lexema)
                self.tabla_lex.insert("", tk.END, values=(num_linea, col, lexema, tipo), tags=('error',) if es_err else ())

            if not tokens_linea:
                continue

            es_asm = (tokens_linea[0] in self.operaciones_asm or (len(tokens_linea) > 2 and tokens_linea[2] == ","))

            # --- 2. ANÁLISIS SINTÁCTICO ---
            hay_error_sintactico = False
            ast_raiz = None

            if hay_error_lexico:
                self.tabla_sin.insert("", tk.END, values=(num_linea, linea_str, "[ERROR ENCADENADO]", "Detenido por ERROR LÉXICO previo"), tags=('error',))
                hay_error_sintactico = True
            elif es_asm:
                op = tokens_linea[0]
                detalle_error = None

                if op not in self.operaciones_asm:
                    detalle_error = "Operación no reconocida para ensamblador"
                elif op in self.asm_unarias:
                    if len(tokens_linea) != 2:
                        detalle_error = f"Formato incorrecto. Esperado: {op} DESTINO"
                    elif not operando_valido.fullmatch(tokens_linea[1]):
                        detalle_error = "Operando inválido: se esperaba registro, identificador o valor"
                else:
                    if len(tokens_linea) != 4 or tokens_linea[2] != ",":
                        detalle_error = f"Formato incorrecto. Esperado: {op} DESTINO, ORIGEN"
                    elif not (operando_valido.fullmatch(tokens_linea[1]) and operando_valido.fullmatch(tokens_linea[3])):
                        detalle_error = "Operando inválido: se esperaba registro, identificador o valor"

                if detalle_error:
                    self.tabla_sin.insert("", tk.END, values=(num_linea, linea_str, "[ERROR SINTÁCTICO]", detalle_error), tags=('error',))
                    hay_error_sintactico = True
                    err_sin += 1
                else:
                    if op in self.asm_unarias:
                        detalle_ok = f"Estructura Correcta ({op} DESTINO)"
                        ast_raiz = NodoAST(op, "INSTRUCCION", "OP_ASM", izq=NodoAST(tokens_linea[1], "OPERANDO", "DESTINO"))
                    else:
                        detalle_ok = f"Estructura Correcta ({op} DESTINO, ORIGEN)"
                        ast_raiz = NodoAST(op, "INSTRUCCION", "OP_ASM",
                                           izq=NodoAST(tokens_linea[1], "OPERANDO", "DESTINO"),
                                           der=NodoAST(tokens_linea[3], "OPERANDO", "ORIGEN"))
                    self.tabla_sin.insert("", tk.END, values=(num_linea, linea_str, "Sintaxis Válida", detalle_ok), tags=('ok',))
            else:
                try:
                    ast_raiz = self.parse_expresion(tokens_linea)
                    if ast_raiz.tipo_nodo == "ASIGNACION":
                        detalle_sin = "Asignación gramaticalmente correcta (ID = expresión)"
                    else:
                        detalle_sin = "Expresión gramaticalmente balanceada"
                    self.tabla_sin.insert("", tk.END, values=(num_linea, linea_str, "Sintaxis Válida", detalle_sin), tags=('ok',))
                except Exception as ex:
                    self.tabla_sin.insert("", tk.END, values=(num_linea, linea_str, "[ERROR SINTÁCTICO]", str(ex)), tags=('error',))
                    hay_error_sintactico = True
                    err_sin += 1

            # --- 3. ANÁLISIS SEMÁNTICO ---
            hay_error_sem = False
            if hay_error_lexico:
                self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, "[ERROR ENCADENADO]", "Detenido por ERROR LÉXICO previo"), tags=('error',))
            elif hay_error_sintactico:
                self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, "[ERROR ENCADENADO]", "Detenido por ERROR SINTÁCTICO previo"), tags=('error',))
            else:
                if es_asm:
                    op = tokens_linea[0]
                    destino = tokens_linea[1]
                    origen = tokens_linea[3] if len(tokens_linea) == 4 else ""

                    est_sem, det_sem, tag_sem = self.validar_semantica_asm(op, destino, origen)
                    if tag_sem == "error":
                        hay_error_sem = True
                        err_sem += 1
                    else:
                        instrucciones_asm.append((num_linea, op, destino, origen))
                    self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, est_sem, det_sem), tags=(tag_sem,))
                else:
                    msg_div = self.buscar_division_cero(ast_raiz)
                    if msg_div:
                        hay_error_sem = True
                        err_sem += 1
                        self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, "[ERROR SEMÁNTICO]", msg_div), tags=('error',))
                    elif ast_raiz.tipo_nodo == "ASIGNACION":
                        self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, "Semántica Válida", f"Asignación a '{ast_raiz.izq.valor}' con operadores coherentes"), tags=('ok',))
                    else:
                        self.tabla_sem.insert("", tk.END, values=(num_linea, linea_str, "Semántica Válida", "Operadores de tipo numérico/aritmético coherentes"), tags=('ok',))

            # --- 4 & 5. ÁRBOL DE LA EXPRESIÓN (las instrucciones ASM van juntas en un solo árbol al final) ---
            if ast_raiz and not es_asm:
                tid = self.arbol_ast.insert("", tk.END, text=f"Línea {num_linea}: {linea_str}", values=("EXPRESION", "AST", "Válido"), tags=('raiz',), open=True)
                self.poblar_treeview(tid, ast_raiz)
                y_cursor_canvas, max_x_alcanzado = self.agregar_arbol_al_canvas(
                    f"Línea {num_linea}: {linea_str}", ast_raiz, y_cursor_canvas, max_x_alcanzado)

            # --- 6 & 7. CÓDIGO INTERMEDIO Y CUÁDRUPLOS (expresiones e instrucciones ASM) ---
            if ast_raiz:
                if hay_error_sem:
                    self.tabla_ci.insert("", tk.END, values=(num_linea, "—", "Sin código: error semántico en la línea"), tags=('error',))
                    self.tabla_cuad.insert("", tk.END, values=(num_linea, "—", "—", "—", "—", "—"), tags=('error',))
                else:
                    gen = GeneradorCodigoIntermedio()
                    gen.generar(ast_raiz)
                    tag_fila = 'par' if num_linea % 2 == 0 else 'impar'

                    if not gen.cuadruplos:
                        self.tabla_ci.insert("", tk.END, values=(num_linea, "—", f"Sin operaciones (operando simple: {ast_raiz.valor})"), tags=('info',))
                        self.tabla_cuad.insert("", tk.END, values=(num_linea, "—", "—", "—", "—", "—"), tags=('info',))
                    for cuad in gen.cuadruplos:
                        idx_cuad += 1
                        op, a1, a2, res = cuad
                        self.tabla_ci.insert("", tk.END, values=(num_linea, idx_cuad, GeneradorCodigoIntermedio.a_tres_direcciones(cuad)), tags=(tag_fila,))
                        self.tabla_cuad.insert("", tk.END, values=(num_linea, idx_cuad, op, a1 or "—", a2 or "—", res or "—"), tags=(tag_fila,))

        # --- ÁRBOL ÚNICO DE ENSAMBLADOR (une todas las instrucciones válidas) ---
        if instrucciones_asm:
            raiz_asm = ConstructorArbolASM().construir(instrucciones_asm)
            lineas_asm = ", ".join(str(i[0]) for i in instrucciones_asm)
            titulo_asm = f"Árbol combinado de ensamblador (líneas {lineas_asm})"

            tid = self.arbol_ast.insert("", tk.END, text=titulo_asm, values=("PROGRAMA ASM", "AST", "Válido"), tags=('raiz',), open=True)
            self.poblar_treeview(tid, raiz_asm)
            y_cursor_canvas, max_x_alcanzado = self.agregar_arbol_al_canvas(
                titulo_asm, raiz_asm, y_cursor_canvas, max_x_alcanzado)

        # Cuadrícula (se envía al fondo para que no tape el árbol)
        alto_total = max(y_cursor_canvas + 50, 700)
        ancho_total = max(max_x_alcanzado + 100, 950)
        self.dibujar_cuadricula(ancho_total, alto_total, tam=25)
        self.canvas.tag_lower("grid")
        self.canvas.config(scrollregion=(0, 0, ancho_total, alto_total))

        # Panel de Estado
        total_err = err_lex + err_sin + err_sem
        if total_err > 0:
            self.lblStatus.config(text=f"⚠️ Detectados: {err_lex} Errores Léxicos | {err_sin} Sintácticos | {err_sem} Semánticos", fg="#B71C1C")
            self.lblDetalle.config(text="Revisa las tablas. Los errores están marcados en rojo con su respectivo diagnóstico.", fg="#B71C1C")
        else:
            self.lblStatus.config(text="✅ Análisis Completado con Éxito", fg="#1B5E20")
            self.lblDetalle.config(text="Fases léxica, sintáctica y semántica superadas. Código intermedio y cuádruplos generados.", fg="#1B5E20")

    def limpiar(self):
        self.txtCodigo.delete(1.0, tk.END)
        for t in [self.tabla_lex, self.tabla_sin, self.tabla_sem, self.arbol_ast, self.tabla_ci, self.tabla_cuad]:
            for item in t.get_children():
                t.delete(item)
        self.canvas.delete("all")
        self.canvas.config(scrollregion=(0, 0, 0, 0))
        self.lblStatus.config(text="Esperando entrada...", fg="#333333")
        self.lblDetalle.config(text="Ingresa expresiones o instrucciones y presiona Analizar", fg="#666666")


if __name__ == "__main__":
    app = AnalizadorDashboard()
    app.mainloop()