module RegisterFile #(ADDR_WIDTH = 1, DATA_WIDTH = 1) (
    input clk,
    input rst,
    input [DATA_WIDTH-1:0] wdata,
    input [ADDR_WIDTH-1:0] waddr,
    input wen,

    input [ADDR_WIDTH-1:0] raddra,
    input [ADDR_WIDTH-1:0] raddrb,//需要能够同时读两个寄存器并且写一个寄存器
    output [DATA_WIDTH-1:0] rdataa,
    output [DATA_WIDTH-1:0] rdatab
);
    reg [DATA_WIDTH-1:0] rf [2**ADDR_WIDTH-1:0];
    always @(posedge clk) begin
        if (wen && (waddr != 5'b00000)) rf[waddr] <= wdata;
        if (rst) for (int i = 0; i < 2**ADDR_WIDTH; i = i + 1) rf[i] <= 0;
    end

    assign rdataa = rf[raddra];
    assign rdatab = rf[raddrb];
endmodule

module risc_V_Reg_file(
    input clk,
    input rst,
    input [4:0] waddr,
    input [31:0] wdata,
    input wen,

    input [4:0] raddra,
    input [4:0] raddrb,

    output [31:0] rdataa,
    output [31:0] rdatab
);

    RegisterFile #(5, 32) rf (
        .clk(clk),
        .rst(rst),
        .wdata(wdata),
        .waddr(waddr),
        .wen(wen),

        .raddra(raddra),
        .raddrb(raddrb),
        .rdataa(rdataa),
        .rdatab(rdatab)
    );
  
    assign rf.rf[0] = 32'h00000000;

endmodule

module risc_V_pc(
    input clk,
    input rst,
    input [31:0] pc_in,
    output [31:0] pc_out
);

    reg [31:0] pc;

    always @(posedge clk or posedge rst) begin
        if (rst) pc <= 32'h80000000;
        else pc <= pc_in;
    end

    assign pc_out = pc;

endmodule

module riscv_V_csr #(
    parameter ADDR_MSTATUS = 12'h300,
    parameter ADDR_MTVEC = 12'h305,
    parameter ADDR_MSCRATCH = 12'h340,
    parameter ADDR_MEPC = 12'h341,
    parameter ADDR_MCAUSE = 12'h342
) (
    input [31:0] test,
    input      clk,
    input      rst,
    input   [11:0]   csr_raddr,
    input   [11:0]   csr_waddr1,
    input   [31:0]   csr_wdata1,
    input   [11:0]   csr_waddr2,
    input   [31:0]   csr_wdata2,
    input   [1:0]   csr_ctr,

    output  [31:0]   csr_output
);

reg [31:0] mstatus, mtvec, mscratch, mepc, mcause, mcache;

assign csr_output = (csr_raddr == ADDR_MSTATUS) ? mstatus :
                    (csr_raddr == ADDR_MTVEC) ? mtvec :
                    (csr_raddr == ADDR_MSCRATCH) ? mscratch :
                    (csr_raddr == ADDR_MEPC) ? mepc :
                    (csr_raddr == ADDR_MCAUSE) ? mcause : 32'h00000000;

always @(posedge clk or posedge rst) begin
    if (rst) begin
        mstatus <= 32'h00000000;
        mtvec <= 32'h00000000;
        mscratch <= 32'h00000000;
        mepc <= 32'h00000000;
        mcause <= 32'h00000000;
    end
    if(csr_ctr==2'b10 || csr_ctr == 2'b11) begin
        case(csr_waddr1)
            ADDR_MSTATUS: mstatus <= csr_wdata1;
            ADDR_MTVEC  : mtvec <= csr_wdata1;
            ADDR_MSCRATCH: mscratch <= csr_wdata1;
            ADDR_MEPC   : mepc <= csr_wdata1;
            ADDR_MCAUSE : mcause <= csr_wdata1;
            default:  mcache <= csr_wdata1;
        endcase
    end
    if(csr_ctr[0] && csr_ctr[1]) begin
        case(csr_waddr2)
            ADDR_MSTATUS: mstatus <= csr_wdata2;
            ADDR_MTVEC  : mtvec <= csr_wdata2;
            ADDR_MSCRATCH: mscratch <= csr_wdata2;
            ADDR_MEPC   : mepc <= csr_wdata2;
            ADDR_MCAUSE : mcause <= csr_wdata2;
            default: mcache <= csr_wdata2;
        endcase
    end
end
    
endmodule //riscv_V_csr

