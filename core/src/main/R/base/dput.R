#  File src/library/base/R/dput.R
#  Part of the R package, http://www.R-project.org
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  A copy of the GNU General Public License is available at
#  http://www.r-project.org/Licenses/

dput <-
    function(x, file = "",
             control = c("keepNA", "keepInteger", "showAttributes"))
{
    if(is.character(file))
        if(nzchar(file)) {
            file <- file(file, "wt")
            on.exit(close(file))
        } else file <- stdout()
    opts <- .deparseOpts(control)
    ## FIXME: this should happen in C {deparse2() in ../../../main/deparse.c}
    ##        but we are missing a C-level slotNames()
    ## Fails e.g. if an S3 list-like object has S4 components
    if(isS4(x)) {
        clx <- class(x)
        cat('new("', clx,'"\n', file = file, sep = '')
	      for(n in .slotNames(clx)) {
	          cat("    ,", n, "= ", file = file)
	          dput(slot(x, n), file = file, control = control)
	      }
	      cat(")\n", file = file)
	      invisible()
    } else {
        cat(deparse(x), file = file, sep = '')
    }
}

dget <- function(file)
    eval(parse(file = file))
