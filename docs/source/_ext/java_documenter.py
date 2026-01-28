import os
import re
import javalang

from sphinx.application import Sphinx

import sphinx
logger = sphinx.util.logging.getLogger('sphinx.ext.autodoc')

def extract_javadoc(comment):
    """
    Parse a Javadoc block into description, params, returns, and throws sections.
    """
    if not comment:
        return "", {}, None, {}

    comment = re.sub(r"^\s*/\*\*|\*/\s*$", "", comment, flags=re.MULTILINE)
    comment = re.sub(r"^\s*\* ?", "", comment, flags=re.MULTILINE)

    description_lines = []
    params, returns, throws = {}, None, {}

    current_tag = None
    for line in comment.splitlines():
        line = line.strip()
        if line.startswith("@param"):
            m = re.match(r"@param\s+(\w+)\s+(.*)", line)
            if m:
                params[m.group(1)] = m.group(2).strip()
                current_tag = ("param", m.group(1))
        elif line.startswith("@return"):
            returns = re.sub(r"@return\s*", "", line).strip()
            current_tag = ("return",)
        elif line.startswith("@throws") or line.startswith("@exception"):
            m = re.match(r"@(throws|exception)\s+(\w+)\s+(.*)", line)
            if m:
                throws[m.group(2)] = m.group(3).strip()
                current_tag = ("throws", m.group(2))
        else:
            # continuation of previous tag or general description
            if current_tag is None:
                description_lines.append(line)
            elif current_tag[0] == "param":
                params[current_tag[1]] += " " + line
            elif current_tag[0] == "return":
                returns += " " + line
            elif current_tag[0] == "throws":
                throws[current_tag[1]] += " " + line

    return description_lines, params, returns, throws


def generate_rst_for_java_file(java_path, package_prefix=None):
    with open(java_path, "r", encoding="utf-8") as f:
        source = f.read()


    tree = javalang.parse.parse(source)
    rst_lines = []
    package_name = tree.package.name if tree.package else package_prefix

    tree.filter(javalang.tree.Import)

    for _, class_decl in tree.filter(javalang.tree.TypeDeclaration):
        if isinstance(class_decl, javalang.tree.EnumDeclaration):
            continue
        full_name = f"{package_name}.{class_decl.name}" if package_name else class_decl.name
        rst_lines.append(f"{class_decl.name}\n{'=' * len(class_decl.name)}\n")

        inheritance = []
        if class_decl.extends:
            inheritance.append(class_decl.extends)

        def _get_subtypes(inherit):
            if inherit.sub_type:
                return "." +inherit.sub_type.name + _get_subtypes(inherit.sub_type)
            else:
                return ""

        for inherit in inheritance:
            subtype = _get_subtypes(inherit)
            for _, imprt in tree.filter(javalang.tree.Import):
                if inherit.name in imprt.path:
                    inheritance[inheritance.index(inherit)] = imprt.path + subtype
                    break
            if inherit in inheritance:
                inheritance[inheritance.index(inherit)] = package_name + "." + inherit.name + subtype

 
        # find nearest preceding comment
        comment = class_decl.documentation

        desc, params, returns, throws = extract_javadoc(comment)

        rst_lines.append(f".. java:class:: {' '.join(class_decl.modifiers)} {full_name}")
        for inherit in inheritance:
            rst_lines.append(f"   :inheritance: {inherit}")
            rst_lines.append("\n")

        for line in desc:
            rst_lines.append(f"   {line}\n")

        # Fields
        for field in class_decl.fields:
            comment = field.documentation
            for declarator in field.declarators:
                fdesc, _, _, _ = extract_javadoc(comment)
                type_name = field.type.name if hasattr(field.type, 'name') else str(field.type)
                rst_lines.append(f"\n   .. java:field:: {type_name} {declarator.name}")
                for fline in fdesc:
                    rst_lines.append(f"\n      {fline}")

        # Constructors
        for ctor in class_decl.constructors:
            sig = f"{ctor.name}({', '.join(p.type.name for p in ctor.parameters)})"
            comment = ctor.documentation
            cdesc, params, returns, throws = extract_javadoc(comment)
            rst_lines.append(f"\n\n   .. java:constructor:: {sig}")
            for cline in cdesc:
                rst_lines.append(f"\n      {cline}")
            for pname, pdesc in params.items():
                rst_lines.append(f"\n      :param {pname}: {pdesc}")
            for tname, tdesc in throws.items():
                rst_lines.append(f"\n      :throws {tname}: {tdesc}")

        # Methods
        for method in class_decl.methods:
            params_sig = ", ".join(
                f"{p.type.name} {p.name}" for p in method.parameters
            )
            sig = f"{' '.join(method.modifiers)} {method.return_type.name if method.return_type else 'void'} {method.name}({params_sig})"
            comment = method.documentation
            mdesc, params, returns, throws = extract_javadoc(comment)
            rst_lines.append(f"\n\n   .. java:method:: {sig}")
            for mline in mdesc:
                rst_lines.append(f"\n      {mline}")
            for pname, pdesc in params.items():
                rst_lines.append(f"\n      :param {pname}: {pdesc}")
            if returns:
                rst_lines.append(f"\n      :return: {returns}")
            for tname, tdesc in throws.items():
                rst_lines.append(f"\n      :throws {tname}: {tdesc}")

        rst_lines.append("\n")

    return "\n".join(rst_lines)


def generate_rst_for_directory(java_dir, output_dir):
    for root, dirs, files in os.walk(java_dir):
        # for this root, we want to get the rel root:
        rel_root = os.path.relpath(root, java_dir)
        out_packages_file = os.path.join(output_dir, rel_root, "packages_index.rst")
        rst_list = []

        for dir in dirs:
            rst_list.append(dir + "/packages_index.rst")

        for file in files:
            if file.endswith(".java"):
                java_path = os.path.join(root, file)
                rel_path = os.path.relpath(java_path, java_dir)
                out_path = os.path.join(output_dir, rel_path.replace(".java", ".rst"))
                rst_list.append(os.path.basename(out_path))
                os.makedirs(os.path.dirname(out_path), exist_ok=True)
                rst = generate_rst_for_java_file(java_path)
                with open(out_path, "w", encoding="utf-8") as f:
                    f.write(rst)
                logger.info(f"Wrote {out_path}")

        title = os.path.basename(root) if root != java_dir else "API Reference"
        rst_lines = title + "\n" + ("=" * len(title)) + "\n\n" + ".. toctree::\n  :maxdepth: 6\n\n  " + "\n  ".join(rst_list) + "\n"

        os.makedirs(os.path.dirname(out_packages_file), exist_ok=True)
        with open(out_packages_file, "w", encoding="utf-8") as f:
            f.write(rst_lines)

        logger.info(f"Wrote {out_packages_file}")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Generate RST from Java sources.")
    parser.add_argument("input", help="Path to directory containing Java files")
    parser.add_argument("output", help="Output directory for RST files")
    args = parser.parse_args()
    generate_rst_for_directory(args.input, args.output)


def setup(app: Sphinx):
    app.add_config_value('java_documenter_source', app.srcdir, 'html')
    project_name = app.config.project
    source_dir = app.config.java_documenter_source
    logger.warning(f"Generating Java docs for project {project_name} from source dir {source_dir}", type='autodoc')
    generate_rst_for_directory(source_dir, os.path.join(app.srcdir, 'api'))