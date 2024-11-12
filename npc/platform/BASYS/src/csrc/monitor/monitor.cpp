#include <utils.hpp>
#include <csignal>
#include <VII_final.h>
#include <nvboard.h>
#include <getopt.h>

bool is_batch_mode = false;

void sdb_set_batch_mode(){
    Log("Entering batch mode");
    is_batch_mode = true;
}

static int parse_args(int argc, char *argv[]) {
    const struct option table[] = {
      {"batch"    , no_argument      , NULL, 'b'},
      {0          , 0                , NULL,  0 },
    };
    int o;
    while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:", table, NULL)) != -1) {
      switch (o) {
        case 'b': sdb_set_batch_mode();     break;
        default:
          printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
          printf("\t-b,--batch              run with batch mode\n");
          printf("\n");
          exit(0);
      }
    }
    return 0;
}

void init_nvboard(void) {
    extern VII_final* top;
    void nvboard_bind_all_pins(VII_final* top);  

    nvboard_bind_all_pins(top);

    nvboard_init();
    
    Log("NVBoard " ANSI_FMT("ON", ANSI_FG_GREEN));
}

void SIGINT_handler(int signal){
    if (signal == SIGINT) {
        printf(ANSI_FMT("\nsimulation interrupted\n", ANSI_FG_BLUE));
        module_state.state = MODULE_STOP;
    }
}

void init_sig(void){
    signal(SIGINT, SIGINT_handler);
}

void Init_wavetrace(int argc, char **argv);
void bram_init(void);
void init_monitor(int argc, char *argv[]) {
    parse_args(argc, argv);

    init_nvboard();

    Init_wavetrace(argc, argv);

    bram_init();

    init_sig();
}

void wave_Trace_close();
void exit(void){
    nvboard_quit();

    wave_Trace_close();
}
