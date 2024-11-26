#include <utils.hpp>
#include <emulator.hpp>
#include <simple_debugger.hpp>

int main(int argc, char **argv) {
    Emulator emulator(argc, argv);

    simple_debugger sdb(emulator, argc, argv);
    sdb.sdb_mainloop();

    // exit();
    return 0;
}
