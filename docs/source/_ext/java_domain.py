"""A Sphinx domain for Java programming language."""
from docutils.parsers.rst import directives

from sphinx import addnodes
from sphinx.domains import Domain, ObjType
from sphinx.domains.python import PyClasslike, PyMethod, PyAttribute
from sphinx.locale import get_translation



class JavaClass(PyClasslike):

    """A custom directive that describes a Java class."""
    option_spec = {
        'inheritance': directives.unchanged,
    }

    has_content = True

    required_arguments = 1

    option_spec = {
        'inheritance': directives.unchanged,
    }

    modifiers = []

    def handle_signature(self, sig, signode):
        if ' ' in sig:
            prefix = sig.strip().split(' ')[:-1]
            sig = sig.split(' ')[-1]
            if len(prefix) >= 1:
                self.modifiers = prefix.copy()
        fullname, prefix = super().handle_signature(sig, signode)
        if 'inheritance' in self.options:
            inheritance = self.options.get('inheritance')
            signode += addnodes.desc_sig_space()
            signode += addnodes.desc_sig_space()
            signode += addnodes.desc_sig_keyword('', 'extends')
            signode += addnodes.desc_sig_space()
            signode += addnodes.desc_sig_name('', inheritance)
        return fullname, prefix

    def get_signature_prefix(self, sig):
        prefix = []
        prefix.extend((
            addnodes.desc_sig_keyword('', ' '.join(self.modifiers)),
            addnodes.desc_sig_space(),
            addnodes.desc_sig_keyword('', 'class'),
            addnodes.desc_sig_space(),
        ))
        return prefix


    def _toc_entry_name(self, sig_node):
        sig = sig_node.astext()
        sig = sig.strip()
        sig = sig.replace(' '.join(self.modifiers), '')
        sig = sig.strip()
        sig = sig.replace('class', '')
        sig = sig.strip()
        sig = sig.split(' ')[0]
        sig = sig.split('.')[-1]
        return sig


class JavaClassMethod(PyMethod):

    """A custom directive that describes a Java class method."""

    modifier = []
    return_type = ""

    def handle_signature(self, sig, signode):
        if ' ' in sig:
            prefix = sig.split('(')[0].split(' ')[:-1]
            sig = sig.split('(')[0].split(' ')[-1] + '(' + sig.split('(')[1]
            if len(prefix) > 1:
                self.modifier = prefix[:-1]
                self.return_type = prefix[-1]
        return super().handle_signature(sig, signode)


    def get_signature_prefix(self, sig):
        prefix = []
        prefix.extend((
            addnodes.desc_sig_keyword('', ' '.join(self.modifier)),
            addnodes.desc_sig_space(),
            addnodes.desc_sig_keyword('', self.return_type),
            addnodes.desc_sig_space(),
        ))
        return prefix


    def _toc_entry_name(self, sig_node):
        sig = sig_node.astext()
        # just the method name
        sig = sig.split('(')[0]
        sig = sig.split(' ')[-1]
        return sig


class JavaField(PyAttribute):

    """A custom directive that describes a Java class field."""

    type = None

    def handle_signature(self, sig, signode):
        if ' ' in sig:
            self.type = sig.split(' ')[0]
            sig = sig.split(' ')[1]

        return super().handle_signature(sig, signode)


    def get_signature_prefix(self, sig):
        prefix = []
        prefix.extend((
            addnodes.desc_sig_keyword('', self.type),
            addnodes.desc_sig_space(),
        ))
        return prefix

    def _toc_entry_name(self, sig_node):
        sig = sig_node.astext()
        # just the class name
        sig = sig.split(' ')[-1]
        return sig


# class JavaXRefRole(XRefRole):
#     def process_link(
#         self,
#         env,
#         refnode,
#         has_explicit_title,
#         title,
#         target,
#     ):
#         refnode['java:class'] = env.ref_context.get('java:class')
#         refnode['java:method'] = env.ref_context.get('java:method')
#         if not has_explicit_title:
#             title = title.lstrip('.')  # only has a meaning for the target
#             target = target.lstrip('~')  # only has a meaning for the title
#             # if the first character is a tilde, don't display the module/class
#             # parts of the contents
#             if title[0:1] == '~':
#                 title = title[1:]
#                 dot = title.rfind('.')
#                 if dot != -1:
#                     title = title[dot + 1 :]
#         # if the first character is a dot, search more specific namespaces first
#         # else search builtins first
#         if target[0:1] == '.':
#             target = target[1:]
#             refnode['refspecific'] = True
#         return title, target



class JavaDomain(Domain):

    """Java language domain."""

    name = 'java'
    label = 'Java'

    object_types = {
        'class':     ObjType(get_translation('package'), 'package', 'ref'),
        'method':    ObjType(get_translation('method'), 'method', 'ref'),
        'field':    ObjType(get_translation('field'), 'field', 'ref'),
    }

    directives = {
        'class':     JavaClass,
        'method':    JavaClassMethod,
        'field':    JavaField,
        'constructor': JavaClassMethod,
    }


    initial_data = {
        'objects': {}, 
    }


# ---------------------------------------------------------------------------
# setup
# ---------------------------------------------------------------------------

def setup(app):

    """Setup the Java domain extension."""

    app.add_domain(JavaDomain)
    return {
        'version': '1.0',
        'parallel_read_safe': True,
        'parallel_write_safe': True,
    }
