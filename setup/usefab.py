from fabric.api import (env, local, run, sudo, get, put,
        abort, require, runs_once, prompt)
from fabric.contrib.files import exists

import string as _string, sys as _sys
v = lambda s: _string.Template(s).safe_substitute(
            env, **_sys._getframe(1).f_locals)

from fabric.main import _internals
_internals.append(v)
