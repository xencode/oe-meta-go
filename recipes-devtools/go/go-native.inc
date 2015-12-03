DESCRIPTION = "Go is an open source programming language that makes it easy to build simple, reliable, and efficient software."
HOMEPAGE = "https://golang.org/"
LICENSE = "BSD-3-Clause"

inherit native

SRC_URI = "http://golang.org/dl/go${PV}.src.tar.gz"
SRC_URI_PREFIX = "http://golang.org/dl/"
GO_BOOTSTRAP_SOURCE="go1.4.src.tar.gz"
SRC_URI_GO_BOOTSTRAP = "${SRC_URI_PREFIX}/${GO_BOOTSTRAP_SOURCE}"
SRC_URI = "${SRC_URI_PREFIX}/go${PV}.src.tar.gz"

S = "${WORKDIR}/go/"

do_compile() {
  ## install a build of Go 1.4 in the SYSROOT so we don't need it anywhere else
  ## in the system (as it currently is the default)
  export GOROOT_BOOTSTRAP_INSTALL="${STAGING_DIR_NATIVE}/go1.4"
  export GOROOT_BOOTSTRAP="${GOROOT_BOOTSTRAP_INSTALL}/go"
  export CC="${HOST_PREFIX}gcc"

  mkdir -p ${GOROOT_BOOTSTRAP_INSTALL}
  cd ${GOROOT_BOOTSTRAP_INSTALL}
  wget ${SRC_URI_GO_BOOTSTRAP}
  tar -xzvf ${GO_BOOTSTRAP_SOURCE}
  cd - && cd ${GOROOT_BOOTSTRAP_INSTALL}/go/src/
  ./all.bash
  cd -

  ## Setting `$GOBIN` doesn't do any good, looks like it ends up copying binaries there.
  export GOROOT_FINAL="${SYSROOT}${libdir}/go"

  cd src && bash -x ./make.bash
}

do_install() {
  ## It turns out that `${D}${bindir}` is already populated by compilation script
  ## We need to copy the rest, unfortunatelly pretty much everything [1, 2].
  ##
  ## [1]: http://sources.gentoo.org/cgi-bin/viewvc.cgi/gentoo-x86/dev-lang/go/go-1.3.1.ebuild?view=markup)
  ## [2]: https://code.google.com/p/go/issues/detail?id=2775

  ## Also `gofmt` is not needed right now.
  install -d "${D}${bindir}"
  install -m 0755 "${WORKDIR}/go/bin/go" "${D}${bindir}"
  install -d "${D}${libdir}/go"
  ## TODO: use `install` instead of `cp`
  for dir in lib pkg src test
  do cp -a "${WORKDIR}/go/${dir}" "${D}${libdir}/go/"
  done

  # Create a symlink so it can be used to bootstrap the cross build
  install -d "${D}${libdir}/go/bin"
  ln -sf "${D}${bindir}/go" "${D}${libdir}/go/bin/go"
}

INHIBIT_PACKAGE_STRIP = "1"
