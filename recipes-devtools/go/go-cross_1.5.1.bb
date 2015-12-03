require go.inc
require go_${PV}.inc

inherit cross

DEPENDS += " go-native"

FILESEXTRAPATHS_prepend := "${THISDIR}/go-${PV}:"

SRC_URI += "\
        file://Fix-ccache-compilation-issue.patch \
        "

do_compile() {
  export GOROOT_BOOTSTRAP="${STAGING_LIBDIR_NATIVE}/go"

  ## Setting `$GOBIN` doesn't do any good, looks like it ends up copying binaries there.
  export GOROOT_FINAL="${SYSROOT}${libdir}/go"

  export GOHOSTOS="linux"
  export GOOS="linux"

  export GOARCH="${TARGET_ARCH}"
  if [ "${TARGET_ARCH}" = "x86_64" ]; then
    export GOARCH="amd64"
  fi
  if [ "${TARGET_ARCH}" = "i586" ]; then
    export GOARCH="386"
  fi  
  if [ "${TARGET_ARCH}" = "arm" ]
  then
    if [ `echo ${TUNE_PKGARCH} | cut -c 1-7` = "cortexa" ]
    then
      echo GOARM 7
      export GOARM="7"
    fi
  fi
  if [ "${TARGET_ARCH}" = "aarch64" ]; then
    export GOARCH="arm64"
  fi
  export CGO_ENABLED="0"
  cd src && bash -x ./make.bash

  ## TODO: consider setting GO_EXTLINK_ENABLED
  # Build standard library with CGO
  export CC="${TARGET_PREFIX}gcc"
  export CGO_CFLAGS="--sysroot=${STAGING_DIR_TARGET} ${TARGET_CC_ARCH}"
  export CXX="${TARGET_PREFIX}gxx"
  export CGO_CXXFLAGS="--sysroot=${STAGING_DIR_TARGET} ${TARGET_CC_ARCH}"
  export CGO_LDFLAGS="--sysroot=${STAGING_DIR_TARGET} ${TARGET_CC_ARCH}"
  export GOROOT="${S}"
  export CGO_ENABLED="1"
  ${S}/bin/go install std


  ## The result is `go env` giving this:
  # GOARCH="amd64"
  # GOBIN=""
  # GOCHAR="6"
  # GOEXE=""
  # GOHOSTARCH="amd64"
  # GOHOSTOS="linux"
  # GOOS="linux"
  # GOPATH=""
  # GORACE=""
  # GOROOT="/home/build/poky/build/tmp/sysroots/x86_64-linux/usr/lib/cortexa8hf-vfp-neon-poky-linux-gnueabi/go"
  # GOTOOLDIR="/home/build/poky/build/tmp/sysroots/x86_64-linux/usr/lib/cortexa8hf-vfp-neon-poky-linux-gnueabi/go/pkg/tool/linux_amd64"
  ## The above is good, but these are a bit odd... especially the `-m64` flag.
  # CC="arm-poky-linux-gnueabi-gcc"
  # GOGCCFLAGS="-fPIC -m64 -pthread -fmessage-length=0"
  # CXX="arm-poky-linux-gnueabi-g++"
  ## TODO: test on C+Go project.
  # CGO_ENABLED="1"
}

do_install() {
  ## It turns out that `${D}${bindir}` is already populated by compilation script
  ## We need to copy the rest, unfortunatelly pretty much everything [1, 2].
  ##
  ## [1]: http://sources.gentoo.org/cgi-bin/viewvc.cgi/gentoo-x86/dev-lang/go/go-1.3.1.ebuild?view=markup)
  ## [2]: https://code.google.com/p/go/issues/detail?id=2775

  ## It should be okay to ignore `${WORKDIR}/go/bin/linux_arm`...
  ## Also `gofmt` is not needed right now.
  install -d "${D}${bindir}"
  install -m 0755 "${WORKDIR}/go/bin/go" "${D}${bindir}"
  install -d "${D}${libdir}/go"
  ## TODO: use `install` instead of `cp`
  for dir in lib pkg src test
  do cp -a "${WORKDIR}/go/${dir}" "${D}${libdir}/go/"
  done
}

INHIBIT_PACKAGE_STRIP = "1"

## TODO: implement do_clean() and ensure we actually do rebuild super cleanly
