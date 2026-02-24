# Use the official SDK image to build the app
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /app

# Copy csproj and restore as distinct layers
COPY ["PromotionAPI.csproj", "./"]
RUN dotnet restore "./PromotionAPI.csproj"

# Copy everything else and build
COPY . .
RUN dotnet publish "PromotionAPI.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Build runtime image
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS final
WORKDIR /app
COPY --from=build /app/publish .

# Copy the CSV files into the container
COPY Files/ /app/Files/

# Configure the port the app listens on
ENV ASPNETCORE_URLS=http://+:5223
EXPOSE 5223

ENTRYPOINT ["dotnet", "PromotionAPI.dll"]
