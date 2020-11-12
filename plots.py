import plotly
import plotly.plotly as py
import plotly.graph_objs as go
import numpy as np

plotly.tools.set_credentials_file(username='simona.kurnavova', api_key='j10Kanj4cCKCPIbeQZ1b')

def readRes(filename, key):
    dataX = []
    dataY = []
    file = open(filename)
    counter = 0

    for line in file:
        if key in line:
            arr = line.split(' ')
            dataX.append(int(counter))
            dataY.append(float(arr[2]))
            counter += 1
    return dataX, dataY

filename = "output"
best_dataX, best_dataY = readRes(filename, "BEST")
avg_dataX, avg_dataY = readRes(filename, "AVG")

trace0 = go.Scatter(
    mode='lines+markers',
    x=best_dataX,
    y=best_dataY,
    name='best'
)

trace1 = go.Scatter(
    mode='lines+markers',
    x=avg_dataX,
    y=avg_dataY,
    name='average'
)

layout = dict(
    legend=dict(
        y=0.5,
        traceorder='reversed',
        font=dict(
            size=16
        )
    ),
    #title='',
    xaxis=dict(title='Epochs'),
    yaxis=dict(title='Fitness'),
)

data = [trace0, trace1]
fig = dict(data=data, layout=layout)
py.iplot(fig, filename='basic-line')