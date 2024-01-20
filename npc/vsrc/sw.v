module absw(
    input clk,
    input rst,
    input a,
    input b,
    output f
);

    reg out_f;

    always @(posedge clk) begin
        if(rst) begin out_f <= 0; end
        else begin
            out_f <= a ^ b;
        end
    end

    assign f = out_f;

endmodule