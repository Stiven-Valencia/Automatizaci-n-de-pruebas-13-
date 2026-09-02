#!/usr/bin/env python3
"""Genera un reporte HTML unificado con los resultados de las pruebas y la cobertura.

Lee los XML que dejan surefire y failsafe, junto con el CSV de JaCoCo, y produce
un unico archivo autocontenido en target/reporte-pruebas.html
"""
import csv
import json
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from html import escape
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
TARGET = RAIZ / "target"
SALIDA = TARGET / "reporte-pruebas.html"

DESCRIPCIONES = {
    "ProductoTest": ("JUnit 5", "Entidad de dominio"),
    "ProductoRepositoryTest": ("@DataJpaTest", "Capa de repositorio"),
    "ProductoServiceTest": ("@SpringBootTest", "Capa de servicio"),
    "ProductoControllerIT": ("@SpringBootTest + MockMvc", "Capa de controlador REST"),
}


def legible(nombre):
    """convertirEsteNombre_DeUnTest -> Convertir este nombre / De un test

    Si la prueba declara @DisplayName, surefire ya escribe una frase legible:
    en ese caso se respeta tal cual.
    """
    if " " in nombre.strip():
        return nombre.strip()
    texto = nombre.replace("_", " / ")
    texto = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", texto)
    return texto[:1].upper() + texto[1:]


def detalle_fallo(caso):
    """Extrae el mensaje de la asercion fallida, si la prueba no paso."""
    for etiqueta in ("failure", "error"):
        nodo = caso.find(etiqueta)
        if nodo is None:
            continue
        mensaje = (nodo.get("message") or "").strip()
        if not mensaje:
            mensaje = (nodo.text or "").strip().splitlines()[0] if nodo.text else ""
        tipo = (nodo.get("type") or "").split(".")[-1]
        texto = " ".join(filter(None, [tipo, mensaje]))
        return texto[:400]
    return ""


def leer_suites():
    suites = []
    for carpeta, motor in (("surefire-reports", "surefire"), ("failsafe-reports", "failsafe")):
        directorio = TARGET / carpeta
        if not directorio.is_dir():
            continue
        for archivo in sorted(directorio.glob("TEST-*.xml")):
            raiz = ET.parse(archivo).getroot()
            clase = raiz.get("name", "").split(".")[-1]
            pruebas = []
            for caso in raiz.findall("testcase"):
                if caso.find("failure") is not None or caso.find("error") is not None:
                    estado = "fallo"
                elif caso.find("skipped") is not None:
                    estado = "omitida"
                else:
                    estado = "ok"
                pruebas.append({
                    "nombre": caso.get("name", ""),
                    "tiempo": float(caso.get("time", 0) or 0),
                    "estado": estado,
                    "detalle": detalle_fallo(caso),
                })
            suites.append({
                "clase": clase,
                "motor": motor,
                "tiempo": float(raiz.get("time", 0) or 0),
                "pruebas": pruebas,
            })
    return suites


SALTO = "\n"

TEXTOS_HTTP = {
    200: "OK", 201: "Created", 204: "No Content", 400: "Bad Request",
    404: "Not Found", 405: "Method Not Allowed", 500: "Internal Server Error",
}


def formatear_json(texto, limite=600):
    """Indenta el JSON respetando los valores originales.

    No se usa json.loads/json.dumps a proposito: eso reinterpretaria los
    numeros y mostraria 2500.0 donde la API devolvio 2500.00. Aqui solo se
    reparte el texto en lineas, sin tocar ningun valor.
    """
    texto = (texto or "").strip()
    if not texto or texto[0] not in "{[":
        return texto

    salida = []
    nivel = 0
    en_cadena = False
    escapando = False

    for caracter in texto:
        if en_cadena:
            salida.append(caracter)
            if escapando:
                escapando = False
            elif caracter == "\\":
                escapando = True
            elif caracter == '"':
                en_cadena = False
            continue

        if caracter == '"':
            en_cadena = True
            salida.append(caracter)
        elif caracter in "{[":
            nivel += 1
            salida.append(caracter + SALTO + "  " * nivel)
        elif caracter in "}]":
            nivel -= 1
            salida.append(SALTO + "  " * nivel + caracter)
        elif caracter == ",":
            salida.append(caracter + SALTO + "  " * nivel)
        elif caracter == ":":
            salida.append(": ")
        elif caracter.isspace():
            continue
        else:
            salida.append(caracter)

    bonito = "".join(salida)
    if len(bonito) > limite:
        bonito = bonito[:limite] + SALTO + "... (recortado)"
    return bonito


def leer_evidencias():
    """Lee las peticiones HTTP reales que registro EvidenciaHttp durante las pruebas."""
    archivo = TARGET / "evidencias.tsv"
    if not archivo.is_file():
        return {}
    registros = {}
    for linea in archivo.read_text(encoding="utf-8").splitlines():
        campos = linea.split("\t")
        if len(campos) < 6:
            continue
        prueba, metodo, ruta, envio, estado, cuerpo = campos[:6]
        registros.setdefault(prueba, []).append({
            "metodo": metodo,
            "ruta": ruta,
            "envio": envio,
            "estado": estado,
            "cuerpo": cuerpo,
        })
    return registros


def leer_evidencias_datos():
    """Lee los resultados reales de las pruebas de repositorio y de servicio."""
    archivo = TARGET / "evidencias-datos.tsv"
    if not archivo.is_file():
        return {}
    registros = {}
    for linea in archivo.read_text(encoding="utf-8").splitlines():
        campos = linea.split("\t")
        if len(campos) < 3:
            continue
        prueba, operacion, resultado = campos[:3]
        registros.setdefault(prueba, []).append({
            "operacion": operacion,
            # Evidencia escribe los saltos como \n literal para caber en una linea
            "resultado": resultado.replace("\\n", SALTO),
        })
    return registros


def leer_cobertura():
    archivo = TARGET / "site" / "jacoco" / "jacoco.csv"
    if not archivo.is_file():
        return [], None
    filas = []
    tot_cubiertas = tot_perdidas = 0
    with archivo.open(encoding="utf-8") as manejador:
        for fila in csv.DictReader(manejador):
            perdidas = int(fila["LINE_MISSED"])
            cubiertas = int(fila["LINE_COVERED"])
            total = perdidas + cubiertas
            tot_cubiertas += cubiertas
            tot_perdidas += perdidas
            filas.append({
                "clase": fila["CLASS"],
                "cubiertas": cubiertas,
                "total": total,
                "pct": (100.0 * cubiertas / total) if total else 0.0,
            })
    filas.sort(key=lambda f: -f["pct"])
    suma = tot_cubiertas + tot_perdidas
    return filas, (100.0 * tot_cubiertas / suma if suma else 0.0)


CSS = """
:root {
  --fondo: #f6f7f9; --tarjeta: #ffffff; --texto: #16181d; --suave: #6b7280;
  --borde: #e4e6eb; --ok: #15803d; --ok-suave: #dcfce7; --fallo: #b91c1c;
  --fallo-suave: #fee2e2; --acento: #1d4ed8;
}
@media (prefers-color-scheme: dark) {
  :root {
    --fondo: #0f1115; --tarjeta: #171a21; --texto: #e8eaed; --suave: #9aa0aa;
    --borde: #262a33; --ok: #4ade80; --ok-suave: #14321f; --fallo: #f87171;
    --fallo-suave: #3b1717; --acento: #60a5fa;
  }
}
* { box-sizing: border-box; }
body {
  margin: 0; padding: 32px 20px; background: var(--fondo); color: var(--texto);
  font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  line-height: 1.5;
}
.envoltorio { max-width: 980px; margin: 0 auto; }
header { margin-bottom: 28px; }
h1 { font-size: 1.6rem; margin: 0 0 6px; letter-spacing: -0.01em; }
.sub { color: var(--suave); font-size: 0.9rem; margin: 0; }
.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 14px; margin: 24px 0 32px; }
.kpi { background: var(--tarjeta); border: 1px solid var(--borde); border-radius: 10px; padding: 16px 18px; }
.kpi .valor { font-size: 1.9rem; font-weight: 650; letter-spacing: -0.02em; }
.kpi .etiqueta { color: var(--suave); font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.05em; margin-top: 2px; }
.kpi.verde .valor { color: var(--ok); }
.kpi.rojo .valor { color: var(--fallo); }
section { background: var(--tarjeta); border: 1px solid var(--borde); border-radius: 10px; padding: 20px 22px; margin-bottom: 18px; }
h2 { font-size: 1.05rem; margin: 0 0 4px; }
.meta { color: var(--suave); font-size: 0.82rem; margin: 0 0 14px; }
.etiqueta-motor { display: inline-block; font-size: 0.72rem; padding: 2px 8px; border-radius: 20px; border: 1px solid var(--borde); color: var(--suave); margin-left: 8px; vertical-align: middle; font-weight: 400; }
table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
th { text-align: left; font-weight: 600; color: var(--suave); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em; padding: 0 8px 8px; border-bottom: 1px solid var(--borde); }
td { padding: 9px 8px; border-bottom: 1px solid var(--borde); }
tr:last-child td { border-bottom: none; }
td.estado { width: 90px; }
td.tiempo { width: 84px; text-align: right; color: var(--suave); font-variant-numeric: tabular-nums; }
.pastilla { display: inline-block; font-size: 0.74rem; font-weight: 600; padding: 2px 9px; border-radius: 20px; }
.pastilla.ok { background: var(--ok-suave); color: var(--ok); }
.pastilla.fallo { background: var(--fallo-suave); color: var(--fallo); }
.barra { background: var(--borde); border-radius: 20px; height: 8px; overflow: hidden; width: 100%; }
.barra > div { height: 100%; border-radius: 20px; background: var(--ok); }
.barra > div.parcial { background: var(--acento); }
td.pct { width: 62px; text-align: right; font-variant-numeric: tabular-nums; font-weight: 600; }
td.barra-celda { width: 45%; }
tr.detalle td { padding-top: 0; border-bottom: 1px solid var(--borde); }
tr.detalle code { display: block; background: var(--fallo-suave); color: var(--fallo); border-radius: 6px; padding: 8px 10px; font-size: 0.78rem; white-space: pre-wrap; word-break: break-word; }
tr.evidencia td { padding-top: 0; }
tr.evidencia pre { margin: 0 0 4px; background: var(--fondo); border: 1px solid var(--borde); border-left: 3px solid var(--acento); border-radius: 6px; padding: 10px 12px; font-size: 0.76rem; line-height: 1.45; overflow-x: auto; font-family: "Cascadia Code", Consolas, "SF Mono", Menlo, monospace; }
tr.evidencia .peticion { color: var(--texto); font-weight: 600; }
tr.evidencia .estado-ok { color: var(--ok); font-weight: 600; }
footer { color: var(--suave); font-size: 0.8rem; text-align: center; margin-top: 26px; }
@media (max-width: 620px) { td.barra-celda { display: none; } body { padding: 20px 12px; } }
"""

PLANTILLA = """<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Reporte de pruebas &mdash; API de Productos</title>
<style>{css}</style>
</head>
<body>
<div class="envoltorio">
<header>
  <h1>Reporte de pruebas de integración</h1>
  <p class="sub">API de Productos &middot; Spring Boot 3.5 &middot; generado el {ahora}</p>
</header>

<div class="kpis">
  <div class="kpi"><div class="valor">{total}</div><div class="etiqueta">Pruebas</div></div>
  <div class="kpi verde"><div class="valor">{exitosas}</div><div class="etiqueta">Exitosas</div></div>
  <div class="kpi {clase_fallos}"><div class="valor">{fallos}</div><div class="etiqueta">Fallidas</div></div>
  <div class="kpi"><div class="valor">{duracion:.1f} s</div><div class="etiqueta">Duración</div></div>
  {kpi_cobertura}
</div>

{suites}
{cobertura}

<footer>Resultado global: {veredicto} &middot; generado por tools/reporte.py</footer>
</div>
</body>
</html>
"""

FILA_PRUEBA = ('<tr><td class="estado"><span class="pastilla {clase}">{txt}</span></td>'
               '<td>{nombre}</td><td class="tiempo">{tiempo:.2f} s</td></tr>')

FILA_EVIDENCIA = ('<tr class="evidencia"><td></td><td colspan="2">'
                  '<pre>{texto}</pre></td></tr>')

FILA_DETALLE = ('<tr class="detalle"><td></td>'
                '<td colspan="2"><code>{texto}</code></td></tr>')

BLOQUE_SUITE = ('<section><h2>{clase}<span class="etiqueta-motor">{motor}</span></h2>'
                '<p class="meta">{capa} &middot; {anotacion} &middot; {n} pruebas en {t:.2f} s</p>'
                '<table><thead><tr><th>Estado</th><th>Prueba</th>'
                '<th style="text-align:right">Tiempo</th></tr></thead>'
                '<tbody>{filas}</tbody></table></section>')

FILA_COBERTURA = ('<tr><td>{clase}</td><td class="barra-celda"><div class="barra">'
                  '<div class="{extra}" style="width:{pct:.0f}%"></div></div></td>'
                  '<td class="tiempo">{cub}/{tot}</td><td class="pct">{pct:.0f}%</td></tr>')

BLOQUE_COBERTURA = ('<section><h2>Cobertura de código</h2>'
                    '<p class="meta">Líneas ejercitadas por las pruebas, medido con JaCoCo</p>'
                    '<table><thead><tr><th>Clase</th><th></th>'
                    '<th style="text-align:right">Líneas</th>'
                    '<th style="text-align:right">%</th></tr></thead>'
                    '<tbody>{filas}</tbody></table></section>')


def generar():
    suites = leer_suites()
    if not suites:
        print("No se encontraron resultados. Ejecuta primero: mvn clean verify")
        return 1

    evidencias = leer_evidencias()
    datos = leer_evidencias_datos()
    cobertura, cobertura_global = leer_cobertura()
    total = sum(len(s["pruebas"]) for s in suites)
    fallos = sum(1 for s in suites for p in s["pruebas"] if p["estado"] == "fallo")
    duracion = sum(s["tiempo"] for s in suites)
    todo_ok = fallos == 0

    partes = []
    for suite in suites:
        anotacion, capa = DESCRIPCIONES.get(suite["clase"], ("", "Pruebas"))
        trozos = []
        for prueba in suite["pruebas"]:
            paso = prueba["estado"] == "ok"
            trozos.append(FILA_PRUEBA.format(
                clase="ok" if paso else "fallo",
                txt="PASA" if paso else "FALLA",
                nombre=escape(legible(prueba["nombre"])),
                tiempo=prueba["tiempo"],
            ))
            for llamada in evidencias.get(prueba["nombre"], []):
                bloque = ['<span class="peticion">{} {}</span>'.format(
                    escape(llamada["metodo"]), escape(llamada["ruta"]))]
                enviado = formatear_json(llamada["envio"])
                if enviado:
                    bloque.append(escape(enviado))
                texto_http = TEXTOS_HTTP.get(int(llamada["estado"]), "")
                bloque.append('<span class="estado-ok">&rarr; {} {}</span>'.format(
                    escape(llamada["estado"]), escape(texto_http)))
                recibido = formatear_json(llamada["cuerpo"])
                if recibido:
                    bloque.append(escape(recibido))
                trozos.append(FILA_EVIDENCIA.format(texto=SALTO.join(bloque)))
            for registro in datos.get(prueba["nombre"], []):
                trozos.append(FILA_EVIDENCIA.format(texto=SALTO.join([
                    '<span class="peticion">{}</span>'.format(escape(registro["operacion"])),
                    '<span class="estado-ok">&rarr;</span> ' + escape(registro["resultado"]),
                ])))
            if prueba["detalle"]:
                trozos.append(FILA_DETALLE.format(texto=escape(prueba["detalle"])))
        filas = "".join(trozos)
        partes.append(BLOQUE_SUITE.format(
            clase=suite["clase"], motor=suite["motor"], capa=capa, anotacion=anotacion,
            n=len(suite["pruebas"]), t=suite["tiempo"], filas=filas,
        ))

    bloque_cobertura = ""
    kpi_cobertura = ""
    if cobertura:
        filas_cobertura = "".join(
            FILA_COBERTURA.format(
                clase=fila["clase"], pct=fila["pct"],
                extra="" if fila["pct"] >= 99.5 else "parcial",
                cub=fila["cubiertas"], tot=fila["total"],
            )
            for fila in cobertura
        )
        bloque_cobertura = BLOQUE_COBERTURA.format(filas=filas_cobertura)
        kpi_cobertura = ('<div class="kpi"><div class="valor">{:.0f}%</div>'
                         '<div class="etiqueta">Cobertura</div></div>').format(cobertura_global)

    documento = PLANTILLA.format(
        css=CSS,
        ahora=datetime.now().strftime("%d/%m/%Y a las %H:%M"),
        total=total,
        exitosas=total - fallos,
        fallos=fallos,
        clase_fallos="verde" if todo_ok else "rojo",
        duracion=duracion,
        kpi_cobertura=kpi_cobertura,
        suites="".join(partes),
        cobertura=bloque_cobertura,
        veredicto="todas las pruebas pasan" if todo_ok else "hay pruebas fallando",
    )

    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(documento, encoding="utf-8")
    print("Reporte generado: {} ({} pruebas, {} fallos)".format(SALIDA, total, fallos))
    return 0


if __name__ == "__main__":
    sys.exit(generar())
