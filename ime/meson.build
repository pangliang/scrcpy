custom_target('scrcpy-ime',
                  # gradle is responsible for tracking source changes
                  build_by_default: true,
                  build_always_stale: true,
                  output: 'scrcpy-ime',
                  command: [find_program('./scripts/build-wrapper.sh'), meson.current_source_dir(), '@OUTPUT@', get_option('buildtype')],
                  console: true,
                  install: true,
                  install_dir: 'share/scrcpy')
